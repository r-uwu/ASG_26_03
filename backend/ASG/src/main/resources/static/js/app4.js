/* ==========================================================================
   app4.js - Integrated Frontend Controller
   [소셜다모아 통합 Frontend Controller]
   [기능: AI 콘텐츠 생성, 업로드, 발행, 복사, 대기 목록/히스토리 관리, 캘린더 연동, 성과 분석 대시보드]
   ========================================================================== */

/* --------------------------------------------------------------------------
   1. Config
   -------------------------------------------------------------------------- */
const PLATFORM_CONFIG = {
  instagram: { label: 'INSTAGRAM', color: '#E1306C', actionType: 'publish', actionLabel: '즉시 발행', icon: '📸' },
  facebook:  { label: 'FACEBOOK',  color: '#1877F2', actionType: 'publish', actionLabel: '즉시 발행', icon: '👤' },
  naver:     { label: 'BLOG',      color: '#03C75A', actionType: 'copy',    actionLabel: '복사 후 발행', icon: '📝' },
  blog:      { label: 'BLOG',      color: '#03C75A', actionType: 'copy',    actionLabel: '복사 후 발행', icon: '📝' },
  kakao:     { label: 'KAKAO',     color: '#FEE500', actionType: 'copy',    actionLabel: '복사 후 발행', icon: '💬' },
  google:    { label: 'GOOGLE',    color: '#EA4335', actionType: 'publish',actionLabel: '구글 리뷰', icon: '⭐' }, // 추가
  youtube:   { label: 'YOUTUBE',   color: '#EF4444', actionType: 'publish',actionLabel: '유튜브', icon: '📹' },   // 추가
  community: { label: 'COMMUNITY', color: '#6366F1', actionType: 'copy',    actionLabel: '복사 후 발행', icon: '💡' }
};

const PASTEL_PALETTE = {
  instagram: { backgroundColor: '#fce4ec', textColor: '#880e4f', borderColor: '#e1306c' },
  facebook: { backgroundColor: '#e3f0fd', textColor: '#0d47a1', borderColor: '#1877f2' },
  naver: { backgroundColor: '#e6f9ee', textColor: '#1b5e20', borderColor: '#03c75a' },
  blog: { backgroundColor: '#e6f9ee', textColor: '#1b5e20', borderColor: '#03c75a' },
  kakao: { backgroundColor: '#fffde7', textColor: '#4a3000', borderColor: '#fee500' },
  community: { backgroundColor: '#ede9fe', textColor: '#3730a3', borderColor: '#6366f1' }
};

/* --------------------------------------------------------------------------
   2. State
   -------------------------------------------------------------------------- */
const state = {
  uploadedImages: [],
  uploadedFiles: [],
  uploadedImageUrl: null,
  generatedContent: {},
  scheduledEvents: [],
  pendingPosts: [],
  calendarInstance: null,
  activeMetrics: new Set(['likes', 'comments']), // 통합 추이 다중선택
  currentPeriod: 'month', // 현재 분석 기간
  compareTab: 'sns',     // 상승도 비교 탭
  chartInstances: {      // 분석 차트 인스턴스
    trend: null, compare: null, donut: null, hourly: null,
    naverTrend: null, naverAge: null, naverGender: null
  },
  currentNaverChart: 'trend', // 네이버 상세 차트 탭
  naverCache: {}          // 네이버 데이터 캐시
};

/* --------------------------------------------------------------------------
   3. API Service
   -------------------------------------------------------------------------- */
const apiService = {
  async fetchDashboardData() {
    try {
      const eventsRes = await fetch('/api/posts/events');
      const events = eventsRes.ok ? await eventsRes.json() : [];
      const pendingRes = await fetch('/api/posts/pending');
      const pending = pendingRes.ok ? await pendingRes.json() : [];
      return { events, pending };
    } catch (err) {
      console.error('[App] fetchDashboardData 오류:', err);
      return { events: [], pending: [] };
    }
  },

  async requestGeneratedContents(payload) {
    const response = await fetch('/api/posts/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error(`서버 오류: ${response.status}`);
    return response.json();
  },

  async publishContent(sns, text, filesArray, externalUrlsArray) {
    const formData = new FormData();
    formData.append('platform', sns);
    formData.append('text', text);
    if (filesArray?.length) filesArray.forEach(file => formData.append('images', file));
    if (externalUrlsArray?.length) externalUrlsArray.forEach(url => formData.append('externalUrls', url));

    const response = await fetch('/api/posts/publish', { method: 'POST', body: formData });
    if (!response.ok) throw new Error(`발행 서버 통신 오류: ${response.status}`);
    return response.json();
  },

  // 분석 데이터 API 호출 추가
  async fetchNaverSearchData(period, from, to) {
    const res = await fetch(`/api/naver-search?period=${period}&from=${from}&to=${to}`);
    if (!res.ok) throw new Error(`네트워크 응답 오류 ${res.status}`);
    return res.json();
  },

  async fetchMindmapData() {
    const res = await fetch(`/api/mindmap`);
    if (!res.ok) throw new Error(`네트워크 응답 오류 ${res.status}`);
    return res.json();
  },

  async fetchPlatformKeywordsData() {
    const res = await fetch(`/api/platform/keywords`);
    if (!res.ok) throw new Error(`네트워크 응답 오류 ${res.status}`);
    return res.json();
  }
};

/* --------------------------------------------------------------------------
   4. UI Manager
   -------------------------------------------------------------------------- */
const uiManager = {
  showToast(message, type = '') {
    const t = document.getElementById('toast');
    if (t) {
      t.textContent = message;
      t.className = `toast show ${type}`;
      setTimeout(() => t.classList.remove('show'), 3200);
    } else alert(message);
  },

  toggleLoading(isLoading, btnId = 'generateBtn', progressId = 'genProgressBar') {
    const btn = document.getElementById(btnId);
    const progress = document.getElementById(progressId);
    if (btn) btn.classList.toggle('loading', isLoading);
    if (progress) progress.classList.toggle('active', isLoading);
  },

  // 성과 분석 대시보드 로딩 표시 추가
  toggleDashboardLoading(isLoading) {
    const overlay = document.getElementById('naverLoadingOverlay');
    const content = document.getElementById('naverContent');
    if (overlay) overlay.style.display = isLoading ? 'block' : 'none';
    if (content) content.style.opacity = isLoading ? '0.4' : '1';
  },

  updatePreview(sns) {
    const resultBox = document.getElementById(`result-${sns}`);
    const emptyBox = document.getElementById(`empty-${sns}`);
    if (resultBox) resultBox.style.display = 'block';
    if (emptyBox) emptyBox.style.display = 'none';
  },

  createStreamRenderer(textEl, cursorEl) {
    textEl.innerHTML = '';
    textEl.appendChild(cursorEl);
    return (char) => {
      const textNode = document.createTextNode(char);
      textEl.insertBefore(textNode, cursorEl);
    };
  },

  openEventModal(info) {
    const modal = document.getElementById('previewModal');
    if (!modal) return;

    //info가 event 객체인지 일반 객체인지 확인
    const props = info.event ? info.event.extendedProps : info;
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');

    if (modalTitle) modalTitle.textContent = info.event ? info.event.title : props.title;
    if (modalBody) {
      modalBody.innerHTML = (props.bodyText || '내용이 없습니다.').replace(/\n/g, '<br>');
    }

    const currentPost = {
      sns: normalizePlatformKey(props.sns || props.platform || '')
    };

    modal.dataset.sns = currentPost.sns;
    updatePublishButtonByPlatform(currentPost);

    modal.classList.remove('hidden');
  },

  closeEventModal() {
    document.getElementById('previewModal')?.classList.add('hidden');
  }
};

window.openEventModal = uiManager.openEventModal;
window.closeEventModal = uiManager.closeEventModal;

/* 플랫폼별 슬라이드 인덱스 관리 상태 */
const carouselState = {};

/* 다중 이미지 슬라이드(캐러셀) DOM 생성기 */
function createCarouselElement(sns, imageUrls) {
  if (!Array.isArray(imageUrls)) {
    imageUrls = [imageUrls].filter(Boolean);
  }
  if (imageUrls.length === 0) return null;

  carouselState[sns] = 0;

  const wrapper = document.createElement('div');
  wrapper.className = 'sns-carousel-wrapper';
  wrapper.style.cssText = 'position: relative; width: 100%; overflow: hidden; border-radius: 12px; margin-bottom: 15px; background: #f1f5f9; aspect-ratio: 1/1;';

  const track = document.createElement('div');
  track.className = 'sns-carousel-track';
  track.style.cssText = 'display: flex; height: 100%; transition: transform 0.3s ease-in-out;';

  imageUrls.forEach(url => {
    const img = document.createElement('img');
    img.src = url;
    img.style.cssText = 'width: 100%; height: 100%; flex-shrink: 0; object-fit: cover;';
    track.appendChild(img);
  });

  wrapper.appendChild(track);

  /* 2장 이상일 경우 좌우 넘기기 버튼 및 인디케이터(점) 생성 */
  if (imageUrls.length > 1) {
    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = '&#10094;';
    prevBtn.style.cssText = 'position: absolute; left: 10px; top: 50%; transform: translateY(-50%); background: rgba(0,0,0,0.5); color: white; border: none; border-radius: 50%; width: 30px; height: 30px; cursor: pointer; display: none; align-items: center; justify-content: center; z-index: 2;';

    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = '&#10095;';
    nextBtn.style.cssText = 'position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: rgba(0,0,0,0.5); color: white; border: none; border-radius: 50%; width: 30px; height: 30px; cursor: pointer; display: flex; align-items: center; justify-content: center; z-index: 2;';

    const dotsWrap = document.createElement('div');
    dotsWrap.style.cssText = 'position: absolute; bottom: 12px; left: 50%; transform: translateX(-50%); display: flex; gap: 6px; z-index: 2;';

    const dots = [];
    imageUrls.forEach((_, idx) => {
      const dot = document.createElement('span');
      dot.style.cssText = `width: 6px; height: 6px; border-radius: 50%; background: ${idx === 0 ? '#fff' : 'rgba(255,255,255,0.5)'}; transition: background 0.3s;`;
      dotsWrap.appendChild(dot);
      dots.push(dot);
    });

    const updateCarouselUI = () => {
      const idx = carouselState[sns];
      track.style.transform = `translateX(-${idx * 100}%)`;
      prevBtn.style.display = idx === 0 ? 'none' : 'flex';
      nextBtn.style.display = idx === imageUrls.length - 1 ? 'none' : 'flex';
      dots.forEach((dot, i) => {
        dot.style.background = i === idx ? '#fff' : 'rgba(255,255,255,0.5)';
      });
    };

    prevBtn.onclick = (e) => {
      e.preventDefault();
      if (carouselState[sns] > 0) {
        carouselState[sns]--;
        updateCarouselUI();
      }
    };

    nextBtn.onclick = (e) => {
      e.preventDefault();
      if (carouselState[sns] < imageUrls.length - 1) {
        carouselState[sns]++;
        updateCarouselUI();
      }
    };

    wrapper.appendChild(prevBtn);
    wrapper.appendChild(nextBtn);
    wrapper.appendChild(dotsWrap);
  }

  return wrapper;
}

/* --------------------------------------------------------------------------
   5. Helpers
   -------------------------------------------------------------------------- */
function getSelectedKeywords() {
  return [...document.querySelectorAll('#keywordGroup input:checked')].map((el) => el.value);
}

function getActiveTones() {
  return [...document.querySelectorAll('#toneGroup .tone-item.active')].map(
    (el) => el.dataset.tone
  );
}

function getActiveEmojiLevel() {
  return document.querySelector('#emojiGroup .slider-item.active')?.dataset.emoji || 'mid';
}

function getActiveSNS() {
  return [...document.querySelectorAll('#snsChips .chip.active')].map((chip) => chip.dataset.sns);
}

function normalizePlatformKey(value) {
  return String(value || '').trim().toLowerCase();
}

function buildGenerateRequestParams() {
  const menu = document.getElementById('menuName')?.value.trim() || '';
  const extra = document.getElementById('extraInfo')?.value.trim() || '';
  const keywords = getSelectedKeywords();
  const activeSNS = getActiveSNS();

  return {
    menu,
    extra,
    keywords,
    activeSNS,
    payload: {
      menuName: menu,
      extraInfo: extra,
      keywords,
      platforms: activeSNS.join(','),
      tones: getActiveTones().join(','),
      emojiLevel: getActiveEmojiLevel(),
      maxLength: document.getElementById('lengthRange') ? parseInt(document.getElementById('lengthRange').value, 10) : 300,
      imageUrl: state.uploadedImageUrl /* 이미지 URL 필드 추가 */
    }
  };
}

function validateGenerateRequest({ menu, keywords, activeSNS }) {
  if (!menu) {
    uiManager.showToast('필수 값(메뉴 / 상품명)을 입력해주세요.');
    return false;
  }
  if (!keywords.length) {
    uiManager.showToast('추가 정보 키워드를 1개 이상 선택해주세요.');
    return false;
  }
  if (!activeSNS.length) {
    uiManager.showToast('발행할 SNS를 1개 이상 선택해 주세요.');
    return false;
  }
  return true;
}

function mergePendingPosts(pendingItems) {
  if (!pendingItems?.length) return;

  const existingIds = new Set(state.pendingPosts.map((post) => String(post.id)));

  pendingItems.forEach((item) => {
    if (existingIds.has(String(item.id))) return;

    const sns = normalizePlatformKey(item.platform);
    state.pendingPosts.push({
      id: item.id,
      sns,
      menu: item.title,
      bodyText: item.content || '',
      label: PLATFORM_CONFIG[sns]?.label || item.platform || '미분류',
      color: item.borderColor || PASTEL_PALETTE[sns]?.borderColor || '#6366F1'
    });
  });
}

function updatePlatformVisibility() {
  const platforms = ['instagram', 'facebook', 'naver', 'kakao', 'community'];
  let firstVisible = null;

  platforms.forEach(sns => {
	const pane = document.getElementById(`pane-${sns}`);
	const tab = document.querySelector(`.tab-btn[data-tab="${sns}"]`);
    const hasContent = state.generatedContent && state.generatedContent[sns];

    if (hasContent) {
      if (tab) {
        tab.style.display = 'flex';
        tab.classList.remove('active'); // 초기화
      }
      if (pane) {
        pane.style.display = ''; // 인라인 블록 강제 속성 제거
        pane.classList.remove('active'); // 초기화
      }
      if (!firstVisible) firstVisible = sns;
    } else {
      if (tab) tab.style.display = 'none';
      if (pane) {
        pane.style.display = 'none';
        pane.classList.remove('active');
      }
    }
  });
  
  

/* */
	  
	  
	  

    if (firstVisible) {
      const activeTab = document.querySelector(`.tab-btn[data-tab="${firstVisible}"]`);
      const activePane = document.getElementById(`pane-${firstVisible}`);
      if (activeTab) activeTab.classList.add('active');
      if (activePane) activePane.classList.add('active');
    } else {
      // 생성된 컨텐츠가 없을 경우 기본 탭 유지
      const igTab = document.querySelector('.tab-btn[data-tab="instagram"]');
      const igPane = document.getElementById('pane-instagram');
      if (igTab) igTab.classList.add('active');
      if (igPane) igPane.classList.add('active');
    }
  }

function animateGeneratedText(sns, text, imageUrls) {
  const textEl = document.getElementById(`text-${sns}`);
  if (!textEl) return;

  textEl.innerHTML = '';

  /* 캐러셀 DOM 주입 */
  const carouselEl = createCarouselElement(sns, imageUrls);
  if (carouselEl) {
    textEl.appendChild(carouselEl);
  }

  const contentWrap = document.createElement('div');
  contentWrap.style.marginTop = '10px';
  textEl.appendChild(contentWrap);

  const cursor = document.createElement('span');
  cursor.className = 'stream-cursor';
  const streamRenderer = uiManager.createStreamRenderer(contentWrap, cursor);

  let index = 0;
  const interval = setInterval(() => {
    if (text[index]) streamRenderer(text[index]);
    index += 1;

    if (index >= text.length) {
      clearInterval(interval);
      uiManager.updatePreview(sns);
      updatePlatformVisibility();
    }
  }, 15);
}

function updatePublishButtonByPlatform(post) {
  const btn = document.getElementById('pmBtnPublish');
  if (!btn || !post) return;

  const snsKey = normalizePlatformKey(post.sns);
  const config = PLATFORM_CONFIG[snsKey];

  btn.dataset.actionType = config?.actionType || 'publish';
  btn.innerHTML = `
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
      <line x1="22" y1="2" x2="11" y2="13" />
      <polygon points="22 2 15 22 11 13 2 9 22 2" />
    </svg>
    ${config?.actionLabel || '즉시 발행'}
  `;
}

/* --------------------------------------------------------------------------
   6. Core Actions
   -------------------------------------------------------------------------- */
window.generateContent = async function () {
  const requestMeta = buildGenerateRequestParams();
  if (!validateGenerateRequest(requestMeta)) return;

  uiManager.toggleLoading(true);
  state.generatedContent = {};

  try {
    const responseData = await apiService.requestGeneratedContents(requestMeta.payload);

    /* responseData 타입에 따른 분기 처리 */
    if (Array.isArray(responseData)) {
      responseData.forEach((res) => {
        const sns = normalizePlatformKey(res.platform);
        const urls = res.imageUrls || (res.imageUrl ? res.imageUrl.split(',') : state.uploadedImages);
        state.generatedContent[sns] = {
          text: res.content,
          imageUrl: urls
        };
        animateGeneratedText(sns, res.content, urls);
      });
    } else {
      requestMeta.activeSNS.forEach((sns) => {
        const text = responseData[sns] || '해당 플랫폼의 생성 결과가 없습니다.';
        state.generatedContent[sns] = { text, imageUrl: state.uploadedImageUrl };
        animateGeneratedText(sns, text, state.uploadedImageUrl);
      });
    }

    uiManager.showToast('AI 콘텐츠 생성이 완료되었습니다.');
  } catch (err) {
    console.error('[App] 콘텐츠 생성 오류:', err);
    uiManager.showToast('콘텐츠 생성 중 오류가 발생했습니다.');
  } finally {
    uiManager.toggleLoading(false);
  }
};

window.publishPost = async function (sns) {
  const content = state.generatedContent[sns];

  if (!content?.text) {
    uiManager.showToast('먼저 AI 콘텐츠를 생성해주세요.');
    return;
  }

  /* pexels URL은 state.uploadedImages 내의 외부 URL을 통해 publishContent에서 처리함 */

  uiManager.toggleLoading(true);
  uiManager.showToast(`${PLATFORM_CONFIG[sns]?.label || sns}에 게시 중입니다. 잠시만 기다려주세요...`);

  try {
    const externalUrls = state.uploadedImages.filter(url => typeof url === 'string' && !url.startsWith('blob:'));
    const result = await apiService.publishContent(
      sns,
      content.text,
      state.uploadedFiles,
      externalUrls
    );

    if (result.status === 'success') {
      uiManager.showToast(`${PLATFORM_CONFIG[sns]?.label || sns}에 성공적으로 게시되었습니다!`);
    } else {
      uiManager.showToast(`게시 실패: ${result.message || '알 수 없는 오류'}`);
    }
  } catch (error) {
    console.error('[App] 발행 통신 오류:', error);
    uiManager.showToast('서버와의 통신 중 오류가 발생했습니다.');
  } finally {
    uiManager.toggleLoading(false);
  }
};

window.copyPostContent = async function (sns) {
  const content = state.generatedContent[sns];

  if (!content?.text) {
    uiManager.showToast('먼저 AI 콘텐츠를 생성해주세요.');
    return;
  }

  try {
    await navigator.clipboard.writeText(content.text);
    uiManager.showToast('복사 완료! 붙여넣어 발행하세요.');
  } catch (error) {
    console.error(error);
    uiManager.showToast('복사 실패');
  }
};

window.handlePmPublishAction = async function () {
  const modal = document.getElementById('previewModal');
  const sns = normalizePlatformKey(modal?.dataset?.sns || '');

  if (!sns) return;

  const config = PLATFORM_CONFIG[sns];

  if (config?.actionType === 'copy') {
    await copyPostContent(sns);
  } else {
    await publishPost(sns);
  }
};

/* --------------------------------------------------------------------------
   7. Pending Posts & Data Synchronization
   -------------------------------------------------------------------------- */
function mapDtoToPost(item) {
  const snsKey = (item.platform || '').toLowerCase();
  const config = PLATFORM_CONFIG[snsKey] || { label: snsKey, color: '#6366F1', icon: '📌' };
  const palette = PASTEL_PALETTE[snsKey] || {
    backgroundColor: '#f1f5f9',
    textColor: '#475569',
    borderColor: '#cbd5e1'
  };

  return {
    id: String(item.id || `pending-${snsKey}-${Date.now()}`),
    sns: snsKey,
    menu: item.menuName || item.title || '제목 없음',
    bodyText: item.content || '',
    hashtags: item.hashtags ? (Array.isArray(item.hashtags) ? item.hashtags : item.hashtags.split(',').map(t => t.trim())) : [],
    imageUrl: item.imageUrl || null,
    originUrl: item.originUrl || null,
    label: config.label || snsKey.toUpperCase(),
    icon: config.icon || '📄',
    color: config.color || palette.borderColor,
    palette: palette
  };
}

function addToPending(sns, menu, bodyText) {
  const newPost = mapDtoToPost({ platform: sns, title: menu, content: bodyText });
  state.pendingPosts.push(newPost);
  renderPendingHTML();
}

function renderPendingHTML() {
  const list = document.getElementById('pendingList');
  const badge = document.getElementById('pendingCountBadge');
  const countText = document.getElementById('pending-count-text');

  if (badge) badge.textContent = state.pendingPosts.length;
  if (countText) countText.textContent = `${state.pendingPosts.length} 건의 대기중`;
  if (!list) return;

  if (!state.pendingPosts.length) {
    list.innerHTML = `
      <div class="pending-empty">
        <div class="pe-icon">📭</div>
        <p>콘텐츠를 생성하면<br>여기에 표시됩니다.</p>
      </div>
    `;
    return;
  }

  list.innerHTML = state.pendingPosts
    .map((post) => {
      if (!post) return '';
      const dotColor = post.palette?.borderColor || post.color || '#6366F1';

      return `
        <div class="pending-item" id="${post.id}" draggable="true"
             ondragstart="onPendingDragStart(event,'${post.id}')"
             ondragend="onPendingDragEnd(event)">
          <span class="pi-dot" style="background-color: ${dotColor} !important;"></span>
          <div class="pi-info">
            <div class="pi-name">${post.icon ? post.icon + ' ' : ''}${post.menu}</div>
            <div class="pi-platform">${post.label}</div>
          </div>
          <div class="pi-drag-hint">드래그</div>
          <button class="pi-remove-btn" type="button" onclick="removePending('${post.id}')">✕</button>
        </div>
      `;
    })
    .join('');
}

window.syncPendingPosts = async function(isManual = false) {
  try {
    const response = await fetch('/api/posts/pending');
    if (!response.ok) throw new Error("데이터 로드 실패");

    const dbData = await response.json();
    state.pendingPosts = dbData.map(item => mapDtoToPost(item));
    renderPendingHTML();

    if (isManual) {
      uiManager.showToast(`${state.pendingPosts.length}개의 포스트를 불러왔습니다.`);
    }
  } catch (error) {
    console.error("Sync Error:", error);
    if (isManual) uiManager.showToast("데이터를 불러오지 못했습니다.");
  }
};

window.loadPendingFromDB = () => window.syncPendingPosts(true);

window.loadCenterHistory = async function() {
  uiManager.showToast("최근 기록을 불러오는 중...");

  try {
    const response = await fetch('/api/posts/pending');
    if (!response.ok) throw new Error("Network response was not ok");

    let data = await response.json();
    if (data.length === 0) return uiManager.showToast("저장된 기록이 없습니다.");

    data.sort((a, b) => b.id - a.id);
    state.generatedContent = {};

    data.forEach(item => {
      const sns = (item.platform || '').toLowerCase();
      if (!state.generatedContent[sns]) {
        const parsedUrls = item.imageUrls ? item.imageUrls : (item.imageUrl ? item.imageUrl.split(',') : []);

        state.generatedContent[sns] = {
          text: item.content,
          imageUrls: parsedUrls,
          hashtags: item.hashtags ? (Array.isArray(item.hashtags) ? item.hashtags : item.hashtags.split(',')) : []
        };

        const textEl = document.getElementById(`text-${sns}`);
        const resultDiv = document.getElementById(`result-${sns}`);
        const emptyDiv = document.getElementById(`empty-${sns}`);

        if (textEl) {
          textEl.innerHTML = '';
          const carouselEl = createCarouselElement(sns, parsedUrls);
          if (carouselEl) textEl.appendChild(carouselEl);

          const textWrap = document.createElement('div');
          textWrap.innerHTML = item.content.replace(/\n/g, '<br>');
          textWrap.style.marginTop = '15px';
          textEl.appendChild(textWrap);
        }

        if (resultDiv) resultDiv.style.display = 'block';
        if (emptyDiv) emptyDiv.style.display = 'none';

        if (parsedUrls.length > 0) {
          state.uploadedImages = [...parsedUrls];
          // updateCenterPreviewsWithImages, renderImagePreviews는 외부 함수 또는 다른 섹션 로직
          if (typeof updateCenterPreviewsWithImages === 'function') updateCenterPreviewsWithImages(parsedUrls);
          renderImagePreviews(); // 좌측 업로드 미리보기도 동기화
        }
      }
    });

    updatePlatformVisibility();
    uiManager.showToast("과거 기록이 중앙 영역에 로드되었습니다.");
  } catch (err) {
    console.error(err);
    uiManager.showToast("기록 로드 실패");
  }
};

window.removePending = function (id) {
  if (typeof CalendarManager !== 'undefined' && CalendarManager.removePending) {
    CalendarManager.removePending(id);
  } else {
    state.pendingPosts = state.pendingPosts.filter(p => p.id !== id);
    renderPendingHTML();
  }
};

/* --------------------------------------------------------------------------
   8. Upload / Retouch & Multi Image Upload
   -------------------------------------------------------------------------- */
let manualUploadedUrls = [];

async function uploadMultipleImagesToServer(files) {
  const formData = new FormData();
  for (let i = 0; i < files.length; i++) {
    formData.append('files', files[i]);
  }
  try {
    const response = await fetch('/api/posts/upload-multiple', {
      method: 'POST',
      body: formData
    });
    if (!response.ok) throw new Error('서버 업로드 실패');
    return await response.json();
  } catch (error) {
    console.error('[Upload Error]:', error);
    throw error;
  }
}

async function handleManualUpload(files) {
  const previewContainer = document.getElementById('imgPreviews');
  if (!previewContainer) return;

  if (files.length > 5) {
    uiManager.showToast('최대 5장까지만 업로드할 수 있습니다.');
    return;
  }
  if (files.length === 0) return;

  previewContainer.innerHTML = '<span style="font-size:13px; color:#64748b;">이미지 업로드 중...</span>';

  try {
    uiManager.toggleLoading(true, 'generateBtn'); //콘텐츠 생성 영역 버튼
    const uploadResult = await uploadMultipleImagesToServer(files);

    if (uploadResult.status === 'success' && uploadResult.urls) {
      manualUploadedUrls.push(...uploadResult.urls);
      state.uploadedImages.push(...uploadResult.urls);
      state.uploadedFiles.push(...Array.from(files));
      state.uploadedImageUrl = state.uploadedImages[0];
      renderImagePreviews();
      if (typeof updateRetouchState === 'function') updateRetouchState();
      uiManager.showToast('이미지가 성공적으로 업로드되었습니다.');
    }
  } catch (e) {
    previewContainer.innerHTML = '<span style="font-size:13px; color:#ef4444;">업로드에 실패했습니다. 다시 시도해주세요.</span>';
  } finally {
    uiManager.toggleLoading(false, 'generateBtn');
  }
}

function handleFiles(files) {
  handleManualUpload(files);
}

function renderImagePreviews() {
  const previewContainer = document.getElementById('imgPreviews');
  if (!previewContainer) return;

  previewContainer.style.display = 'flex';
  previewContainer.style.gap = '10px';
  previewContainer.style.flexWrap = 'wrap';
  previewContainer.style.marginTop = '12px';

  previewContainer.innerHTML = state.uploadedImages
    .map(
      (src, i) => `
        <div class="img-thumb-wrap" style="position:relative;">
          <img class="img-thumb" src="${src}" alt="" style="width:80px; height:80px; object-fit:cover; border-radius:8px; border:1px solid #e2e8f0;">
          <button class="remove-btn" type="button" onclick="removeImg(${i})" style="position:absolute; top:-5px; right:-5px; background:#ef4444; color:#fff; border:none; border-radius:50%; width:20px; height:20px; cursor:pointer; font-size:12px; line-height:1;">×</button>
        </div>
      `
    )
    .join('');
}

window.updateRetouchState = function() {
  const activeSNS = getActiveSNS();
  const enabled =
    (activeSNS.includes('instagram') || activeSNS.includes('facebook')) &&
    state.uploadedImages.length > 0;

  const btn = document.getElementById('retouchBtn');
  if (btn) btn.disabled = !enabled;
}

window.removeImg = function (index) {
  const removedUrl = state.uploadedImages[index];
  const manualIndex = manualUploadedUrls.indexOf(removedUrl);
  if (manualIndex > -1) {
    manualUploadedUrls.splice(manualIndex, 1);
    state.uploadedFiles.splice(manualIndex, 1);
  }

  state.uploadedImages.splice(index, 1);
  state.uploadedImageUrl = state.uploadedImages.length > 0 ? state.uploadedImages[0] : null;

  renderImagePreviews();
  updateRetouchState();
};

window.openRetouchModal = function () {
  document.getElementById('retouchModal')?.classList.add('open');
};

window.closeRetouchModal = function () {
  document.getElementById('retouchModal')?.classList.remove('open');
};

window.applyRetouchAndClose = function () {
  window.closeRetouchModal();
};

/* --------------------------------------------------------------------------
   9. UI Bindings
   -------------------------------------------------------------------------- */
const rangeEl = document.getElementById('lengthRange');
const rangeValEl = document.getElementById('rangeVal');
const menuInput = document.getElementById('menuName');
const menuCount = document.getElementById('menuCount');
const uploadZone = document.getElementById('uploadZone');
const imgInput = document.getElementById('imgInput');

function updateRange() {
  if (!rangeEl || !rangeValEl) return;
  const pct = ((rangeEl.value - rangeEl.min) / (rangeEl.max - rangeEl.min)) * 100;
  rangeEl.style.setProperty('--pct', `${pct}%`);
  rangeValEl.textContent = `${rangeEl.value}자`;
}

function bindFormEvents() {
  const form = document.getElementById('contentForm');
  form?.addEventListener('submit', (e) => e.preventDefault());
}

function bindRangeEvents() {
  if (!rangeEl) return;
  rangeEl.addEventListener('input', updateRange);
  updateRange();
}

function bindMenuCounterEvents() {
  if (!menuInput || !menuCount) return;

  menuInput.addEventListener('input', () => {
    const len = menuInput.value.length;
    menuCount.textContent = `${len}/50`;
    menuCount.classList.toggle('warn', len > 40);
  });
}

function bindToneEvents() {
  document.querySelectorAll('#toneGroup .tone-item').forEach((item) => {
    item.addEventListener('click', (e) => {
      e.preventDefault(); // 기본 이벤트 차단
      item.classList.toggle('active');

      if (!document.querySelectorAll('#toneGroup .tone-item.active').length) {
        document
          .querySelector('#toneGroup .tone-item[data-tone="default"]')
          ?.classList.add('active');
      }
    });
  });
}

function bindEmojiEvents() {
  document.querySelectorAll('#emojiGroup .slider-item').forEach((item) => {
    item.addEventListener('click', (e) => {
      e.preventDefault(); // 기본 이벤트 차단
      document
        .querySelectorAll('#emojiGroup .slider-item')
        .forEach((node) => node.classList.remove('active'));
      item.classList.add('active');
    });
  });
}

function bindTabEvents() {
  const tabButtons = document.querySelectorAll('.tab-btn');
  const panes = document.querySelectorAll('.output-pane');

  tabButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      tabButtons.forEach((b) => b.classList.remove('active'));
      panes.forEach((pane) => pane.classList.remove('active'));

      btn.classList.add('active');
      const paneId = `pane-${btn.dataset.tab}`;
      const pane = document.getElementById(paneId);
      if (pane) {
          pane.style.display = ''; //display block 등으로 강제 지정 방지
          pane.classList.add('active');
      }
    });
  });
}

function bindSnsChipEvents() {
  document.querySelectorAll('#snsChips .chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      chip.classList.toggle('active');
      updateRetouchState();
    });
  });
}

function bindUploadEvents() {
  uploadZone?.addEventListener('click', () => imgInput?.click());

  uploadZone?.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.style.borderColor = 'rgba(20,184,166,0.35)';
  });

  uploadZone?.addEventListener('dragleave', () => {
    uploadZone.style.borderColor = '';
  });

  uploadZone?.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.style.borderColor = '';
    if (e.dataTransfer.files) handleFiles(e.dataTransfer.files);
  });

  imgInput?.addEventListener('change', () => handleFiles(imgInput.files));
}

// 사용자 설정 온클릭 작동을 위한 바인딩 함수 통합
function bindUserSettings() {
  bindToneEvents();
  bindEmojiEvents();
  // 대시보드 쪽 기간/KPI 버튼 등은 HTML onclick을 사용하므로 생략
}

function bindAllUIEvents() {
  bindFormEvents();
  bindRangeEvents();
  bindMenuCounterEvents();
  bindUserSettings(); // 통합된 사용자 설정 바인딩 호출
  bindTabEvents();
  bindSnsChipEvents();
  bindUploadEvents();
}


/* Modal Control Functions */
function openPexelsModal() {
    document.getElementById('pexelsModal').style.display = 'flex';
}

function closePexelsModal() {
    document.getElementById('pexelsModal').style.display = 'none';
}

/* Close modal on outside click */
window.addEventListener('click', function(e) {
    const pexelsModal = document.getElementById('pexelsModal');
    if (e.target === pexelsModal) {
        closePexelsModal();
    }
});

/* --------------------------------------------------------------------------
   10. Dashboard & Analytics (Second Code integration)
   -------------------------------------------------------------------------- */
const dashboardRenderer = {
  getTopPlatforms(scores, count = 3) {
    return [
      { id: 'instagram', name: 'Instagram', color: '#E1306C' },
      { id: 'facebook', name: 'Facebook', color: '#1877F2' },
      { id: 'naver', name: '네이버 블로그', color: '#03C75A' },
      { id: community, name: '커뮤니티', color: community.borderColor },
    ]
    .map((p, i) => ({ ...p, score: scores[i] || 0 }))
    .sort((a, b) => b.score - a.score)
    .slice(0, count);
  },

  getContentTypeByPlatform(platformId) {
    const map = {
      instagram: '비주얼 중심 이벤트형 게시물',
      facebook: '정보형·공지형 게시물',
      naver: '리뷰형·상세 소개형 포스트',
      google: '리뷰 유도형 콘텐츠',
      kakao: '재방문 유도형 메시지',
    };
    return map[platformId] || '일반 홍보형 콘텐츠';
  },

  renderNaverKeywords(keywords) {
    const el = document.getElementById('keywordList');
    if (!el) return;
    if (!keywords || keywords.length === 0) {
      el.innerHTML = '<div style="padding:12px 0;color:var(--text3);font-size:13px;text-align:center;">데이터가 없습니다</div>';
      return;
    }
    const max = keywords[0].searchCount || 1;
    el.innerHTML = keywords.map((k, i) => `
      <div class="keyword-row">
        <span class="kw-rank">${i + 1}</span>
        <span class="kw-name">${k.keywordText}</span>
        <div class="kw-bar-bg">
          <div class="kw-bar-fill" style="width:0%" data-target="${Math.round(k.searchCount / max * 100)}"></div>
        </div>
        <span class="kw-val">${k.searchCount.toLocaleString()}</span>
      </div>
    `).join('');
    setTimeout(() => {
      document.querySelectorAll('.kw-bar-fill').forEach(b => {
        b.style.width = (b.dataset.target || '80') + '%';
      });
    }, 200);
  },

  renderNaverSummary(summary, period) {
    if (!summary) return;
    const sEl = document.getElementById('naverSearchVal');
    const sSub = document.getElementById('naverSearchSub');
    const uEl = document.getElementById('naverUserStat');
    const uSub = document.getElementById('naverUserStatSub');

    if (sEl) {
      const score = summary.totalSearchCount ?? 0;
      const status = summary.searchActivityStatus ?? '';
      const statusIcon = { '폭발적': '🔥', '안정적': '✅', '침체기': '📉' }[status] ?? '';
      sEl.innerHTML = `
        <span style="font-size:1.6rem;font-weight:800;">${score}pt</span>
        <span style="font-size:1rem;font-weight:600;margin-left:6px;color:var(--text-sub);">
          (${statusIcon} ${status})
        </span>`;
    }
    if (sSub) {
      const score = summary.totalSearchCount ?? 0;
      const growthPct = summary.searchGrowthPct ?? 0;
      const ptDiff = Math.round(score * growthPct / (100 + growthPct));
      const up = ptDiff >= 0;
      sSub.innerHTML = `전월 대비 <span style="color:${up ? 'var(--green)' : 'var(--red)'};font-weight:700;">${up ? '▲' : '▼'} ${Math.abs(ptDiff)}pt</span>`;
    }

    if (uEl) {
      if (period === 'week') uEl.textContent = '주간 데이터는 제공하지 않습니다';
      else if (summary.topUser) uEl.textContent = summary.topUser + '이 가장 많이 검색';
      else uEl.textContent = '데이터가 없습니다';
    }
    if (uSub) {
      if (period === 'week') uSub.textContent = '';
      else if (summary.prevTopUser) uSub.textContent = '전월: ' + summary.prevTopUser + '이 가장 많이 검색';
      else uSub.textContent = '';
    }
  },

  // HTMLonclick에서 호출하는 함수들
  setPeriod(period) {
    state.currentPeriod = period;
    document.querySelectorAll('.period-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.period === period);
    });
    // 대시보드 업데이트 로직 호출 (buildAllCharts, loadNaverData 등)
    if(typeof updateAllCharts === 'function') updateAllCharts();
    if(typeof loadNaverData === 'function') loadNaverData(period);
  },

  setKpi(el) {
    document.querySelectorAll('.kpi-item').forEach(item => item.classList.remove('active'));
    el.classList.add('active');
  },

  toggleMetric(metric) {
    if (state.activeMetrics.has(metric)) {
      if (state.activeMetrics.size === 1) return; // 최소 1개 유지
      state.activeMetrics.delete(metric);
    } else {
      state.activeMetrics.add(metric);
    }
    const chip = document.querySelector(`.mchip[data-metric="${metric}"]`);
    if (chip) chip.classList.toggle('active', state.activeMetrics.has(metric));
    // 트렌드 차트 업데이트 호출
    if(typeof updateTrendChart === 'function') updateTrendChart();
  },

  switchCompareTab(tab) {
    state.compareTab = tab;
    const snsBtnEl = document.getElementById('compareTabSns');
    const reviewBtnEl = document.getElementById('compareTabReview');
    const metricSelEl = document.getElementById('compareMetricSel');

    const activeStyle = 'padding:4px 12px;border-radius:100px;border:none;font-size:11px;font-weight:700;cursor:pointer;font-family:\'Noto Sans KR\',sans-serif;background:linear-gradient(135deg,var(--teal),var(--indigo));color:#fff;box-shadow:0 2px 6px rgba(20,184,166,.25);';
    const inactiveStyle = 'padding:4px 12px;border-radius:100px;border:none;font-size:11px;font-weight:700;cursor:pointer;font-family:\'Noto Sans KR\',sans-serif;background:transparent;color:var(--text3);';

    if (tab === 'sns') {
      if(snsBtnEl) snsBtnEl.style.cssText = activeStyle;
      if(reviewBtnEl) reviewBtnEl.style.cssText = inactiveStyle;
      if(metricSelEl) metricSelEl.style.display = '';
    } else {
      if(snsBtnEl) snsBtnEl.style.cssText = inactiveStyle;
      if(reviewBtnEl) reviewBtnEl.style.cssText = activeStyle;
      if(metricSelEl) metricSelEl.style.display = 'none';
    }
    if(typeof updateCompareChart === 'function') updateCompareChart();
  },

  switchNaverChart() {
    const sel = document.getElementById('naverChartTypeSel');
    if(!sel) return;
    state.currentNaverChart = sel.value;
    const panes = ['Trend', 'Age', 'Gender'];
    panes.forEach(pane => {
        const el = document.getElementById(`naver${pane}Chart`);
        if(el) el.style.display = state.currentNaverChart === pane.toLowerCase() ? 'block' : 'none';
    });
  }
};

// HTML onclick에서 전역 호출 가능하도록 바인딩
window.setPeriod = dashboardRenderer.setPeriod;
window.setKpi = dashboardRenderer.setKpi;
window.toggleMetric = dashboardRenderer.toggleMetric;
window.switchCompareTab = dashboardRenderer.switchCompareTab;
window.switchNaverChart = dashboardRenderer.switchNaverChart;

// 분석 데이터 로드 (Second Code 로직)
window.loadNaverData = async function(period) {
  uiManager.toggleDashboardLoading(true);

  if (state.naverCache[period]) {
    uiManager.toggleDashboardLoading(false);
    renderNaverAll(state.naverCache[period], period);
    return;
  }

  try {
    const from = new Date(); from.setMonth(from.getMonth() - (period === 'year' ? 12 : period === 'month' ? 1 : 0)); if(period === 'week') from.setDate(from.getDate() - 7);
    const to = new Date();
    const fmt = d => d.toISOString().slice(0, 10);
    const data = await apiService.fetchNaverSearchData(period, fmt(from), fmt(to));
    state.naverCache[period] = data;
    renderNaverAll(data, period);
  } catch (err) {
    console.error('네이버 데이터 로드 실패:', err);
    const el = document.getElementById('naverSearchVal');
    if (el) el.textContent = '오류';
    uiManager.showToast('네이버 데이터 로드에 실패했습니다.');
  } finally {
    uiManager.toggleDashboardLoading(false);
  }
};

window.generateStrategy = async function() {
  const btn = document.getElementById('aiGenBtn');
  if(!btn) return;
  uiManager.toggleLoading(true, 'aiGenBtn');
  const btnText = btn.querySelector('.btn-text');
  if(btnText) btnText.textContent = '분석 중...';
  
  document.getElementById('aiEmpty').style.display = 'none';
  document.getElementById('aiResult').style.display = 'block';
  document.getElementById('aiCards').innerHTML = '';
  document.getElementById('aiSummaryText').textContent = '';
  document.getElementById('aiSummaryText').classList.remove('typing-cursor');

  try {
    await new Promise(resolve => setTimeout(resolve, 650));
    const dataRes = await fetch('/api/dashboard/summary?brandId=18'); //브랜드아이디 하드코딩 제거 필요
    const parsed = await dataRes.json();
    const recommendations = parsed.recommendations || [];

    document.getElementById('aiCards').innerHTML = recommendations.map((r, i) => `
      <div class="ai-rec-card">
        <div class="ai-rec-header">
          <div class="ai-rec-num">${i + 1}</div>
          <div class="ai-rec-title">${r.title}</div>
        </div>
        <div class="ai-rec-body">
          <strong>📢 채널:</strong> ${r.channel}<br>
          <strong>⏰ 시간:</strong> ${r.time}<br>
          <strong>📝 유형:</strong> ${r.type}<br><br>
          ${r.detail}
        </div>
      </div>
    `).join('');

    const sumEl = document.getElementById('aiSummaryText');
    const txt = parsed.summary || '';
    sumEl.classList.add('typing-cursor');
    let i = 0;
    const iv = setInterval(() => {
      sumEl.textContent = txt.slice(0, ++i);
      if (i >= txt.length) {
        clearInterval(iv);
        sumEl.classList.remove('typing-cursor');
      }
    }, 18);

    uiManager.showToast('전략 분석이 완료되었습니다!', 'success');
  } catch (error) {
    document.getElementById('aiEmpty').style.display = 'flex';
    document.getElementById('aiResult').style.display = 'none';
    uiManager.showToast('분석 중 오류가 발생했습니다. 다시 시도해 주세요.');
    console.error(error);
  } finally {
      uiManager.toggleLoading(false, 'aiGenBtn');
      if(btnText) btnText.textContent = '✦ 전략 분석 시작';
  }
};

// 차트 빌드, 애니메이션 등 나머지 분석 로직은 safeBuildCharts, renderNaverAll 등 함수 내에 구현 (Second Code 참조)
// safeBuildCharts 등 함수 정의 (생략, Second Code 로직 그대로 사용)

/* --------------------------------------------------------------------------
   11. Dashboard Init
   -------------------------------------------------------------------------- */
async function initializeDashboard() {
  if (typeof CalendarManager !== 'undefined') {
    try {
      CalendarManager.init({
        state,
        uiManager,
        PLATFORM_CONFIG,
        PASTEL_PALETTE,
        renderPendingHTML,
        publishPost: window.publishPost
      });
      CalendarManager.initCalendar();
      console.log("[Dash] Calendar initialized");
    } catch (e) {
      console.error("[Dash] Calendar Init Error:", e);
    }
  }
  updateRetouchState();
}

// ── 마이페이지 기본값 pre-select ──────────────────────────
function applyContentDefaults() {
  const tone  = document.getElementById('cs_tone')?.value;
  const emoji = document.getElementById('cs_emojiLevel')?.value;
  const len   = document.getElementById('cs_length')?.value;
  const sns   = document.getElementById('cs_sns')?.value;

  if (!tone && !emoji && !len && !sns) return;

  const TONE_MAP = { '기본':'default', '친근':'friendly', '깔끔':'clean', '격식':'formal', '트렌디':'trendy' };
  if (tone && TONE_MAP[tone]) {
    document.querySelectorAll('#toneGroup .tone-item').forEach(el => {
      el.classList.toggle('active', el.dataset.tone === TONE_MAP[tone]);
    });
  }

  const EMOJI_MAP = { '적게':'low', '적당히':'mid', '많이':'high' };
  if (emoji && EMOJI_MAP[emoji]) {
    document.querySelectorAll('#emojiGroup .slider-item').forEach(el => {
      el.classList.toggle('active', el.dataset.emoji === EMOJI_MAP[emoji]);
    });
  }

  if (len && rangeEl && rangeValEl) {
    rangeEl.value = len;
    rangeValEl.textContent = len + '자';
    updateRange();
  }

  if (sns) {
    const selected = sns.split(',').map(s => s.trim());
    document.querySelectorAll('#snsChips .chip').forEach(el => {
      el.classList.toggle('active', selected.includes(el.dataset.sns));
    });
  }
}

/* ── 키워드 동적 렌더링 함수 ────────────────────────── */
async function fetchAndRenderKeywords(industryCode) {
  const keywordGroup = document.getElementById('keywordGroup');
  if (!keywordGroup) return;

  try {
      keywordGroup.innerHTML = `
          <div style="width:100%; padding:10px; font-size:12px; color:#64748b; text-align:center;">
              키워드 데이터를 불러오는 중...
          </div>
      `;

      const response = await fetch(`/api/keywords?industryCode=${industryCode}`);
      if (!response.ok) throw new Error("네트워크 응답 에러");
      
      const keywords = await response.json();

      if (!keywords || keywords.length === 0) {
          keywordGroup.innerHTML = `
              <div style="width:100%; padding:10px; font-size:12px; color:#64748b; text-align:center;">
                  현재 업종에 등록된 추천 키워드가 없습니다.
              </div>
          `;
          return;
      }

      keywordGroup.innerHTML = keywords.map((kw, index) => `
          <div class="keyword-item">
              <input type="checkbox" name="keywords" id="kw-${index}" value="${kw.name}">
              <label class="keyword-label" for="kw-${index}">${kw.name}</label>
          </div>
      `).join('');

  } catch (error) {
      console.error("키워드 로드 실패:", error);
      keywordGroup.innerHTML = `
          <div style="width:100%; padding:10px; font-size:12px; color:red; text-align:center;">
              키워드를 불러오는데 실패했습니다.
          </div>
      `;
  }
}

// ── 단일화된 Boot Sequence (초기화) ──────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  try {
    // 1. UI 클릭 이벤트 바인딩 (통합된 bindAllUIEvents 호출)
    bindAllUIEvents();

    // 2. 마이페이지 기본값 세팅 (Hidden Input 기반)
    applyContentDefaults();

    // 3. 업종별 추천 키워드 로드
    const hiddenInput = document.getElementById("myIndustryCode");
    const myIndustryCode = hiddenInput ? hiddenInput.value : null;
    if (myIndustryCode) {
        await fetchAndRenderKeywords(myIndustryCode);
    }

    // 4. 캘린더 및 대시보드 초기화
    await initializeDashboard();

    // 5. 성과 분석 대시보드 초기 데이터 로드 (Second Code 로직)
    // safeBuildCharts();
    // if(typeof renderCompareTable === 'function') renderCompareTable();
    // animateKpis(); //KPI 애니메이션
    // loadNaverData('month'); //기본 월간 데이터 로드

	
	const defaultPlatform = "instagram"; 
	const tabButtons = document.querySelectorAll('.tab-btn');
	    tabButtons.forEach(btn => {
	        if(btn.getAttribute('data-tab') === defaultPlatform) {
	            btn.classList.add('active');
	        } else {
	            btn.classList.remove('active');
	        }
	    });
		
		const activePane = document.getElementById('pane-' + defaultPlatform);
		   if(activePane) {
		       activePane.style.display = 'block';
		       
		       /* Ensure empty state is visible and result state is hidden */
		       const emptyState = activePane.querySelector('.empty-state');
		       const resultState = activePane.querySelector('#result-' + defaultPlatform);
		       
		       if(emptyState) emptyState.style.display = 'block';
		       if(resultState) resultState.style.display = 'none';
		   }
		
			
		
  } catch (error) {
    console.error("Boot Sequence Error:", error);
  }
});