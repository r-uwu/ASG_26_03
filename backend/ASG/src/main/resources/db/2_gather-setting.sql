/*
  2_gather-setting.sql
  카페 디아즈 / 카페 산정 — 로그인 후 계정 연결
  실행 전제: 0_gather-setting.sql → 1_gather-setting.sql 실행 완료

  ══════════════════════════════════════════════════════════════════
  전체 실행 순서
  ══════════════════════════════════════════════════════════════════

  STEP 1.  0_gather-setting.sql 실행
  STEP 2.  1_gather-setting.sql 실행
  STEP 3.  구글 계정 A 로 로그인 → 회원가입 절차 완료
  STEP 4.  구글 계정 B 로 로그인 → 회원가입 절차 완료
  STEP 5.  아래 확인 쿼리로 각 계정의 id 확인

           SELECT id, email, provider, provider_id
           FROM users
           ORDER BY id ASC;

  STEP 6.  아래 SET 세 줄을 실제 id 값으로 수정 후 이 파일 전체 실행
			
		SET @DIAZ_USER_ID     = ??;  ← 카페 디아즈로 쓸 users.id
		SET @SANJEONG_USER_ID = ??;  ← 카페 산정으로 쓸 users.id

  ══════════════════════════════════════════════════════════════════
  이 파일이 하는 것
  ══════════════════════════════════════════════════════════════════
  ① 회원가입으로 새로 생긴 brand, brand_platform,
     business_hours, content_settings 전부 삭제
  ② brand(id=1,2) 에 실제 users.id 연결
  ③ users 정보를 디아즈/산정 더미값으로 덮어쓰기
  ④ business_hours, content_settings 더미값으로 재생성
  ⑤ inquiry 더미 생성 (마이페이지 문의 탭 확인용)
  ══════════════════════════════════════════════════════════════════
*/

USE gather;
SET FOREIGN_KEY_CHECKS = 0;

-- ──────────────────────────────────────────────────────────────────
-- 실행 전 아래 두 값 (1; 2;)을 실제 users.id 로 교체
-- user.id 1번이 admin 계정으로 설정된 korjje617@gmail.com 이라는 가정 하에
-- ──────────────────────────────────────────────────────────────────
SET @DIAZ_USER_ID     = 2; -- 카페 디아즈 계정
SET @SANJEONG_USER_ID = 3; -- 카페 산정 계정


-- ══════════════════════════════════════════════
-- 1. 회원가입으로 새로 생긴 데이터 전부 삭제
-- ══════════════════════════════════════════════

DELETE FROM brand_platform
WHERE brand_id IN (SELECT brand_id FROM brand WHERE user_id = @DIAZ_USER_ID);

DELETE FROM brand WHERE user_id = @DIAZ_USER_ID;

DELETE FROM brand_platform
WHERE brand_id IN (SELECT brand_id FROM brand WHERE user_id = @SANJEONG_USER_ID);

DELETE FROM brand WHERE user_id = @SANJEONG_USER_ID;

DELETE FROM business_hours  WHERE user_id = @DIAZ_USER_ID;
DELETE FROM business_hours  WHERE user_id = @SANJEONG_USER_ID;
DELETE FROM content_settings WHERE user_id = @DIAZ_USER_ID;
DELETE FROM content_settings WHERE user_id = @SANJEONG_USER_ID;


-- ══════════════════════════════════════════════
-- 2. brand(id=1,2) 에 실제 users.id 연결
-- ══════════════════════════════════════════════

UPDATE brand SET user_id = @DIAZ_USER_ID     WHERE brand_id = 1; -- 카페 디아즈가 브랜드 1번
UPDATE brand SET user_id = @SANJEONG_USER_ID WHERE brand_id = 2; -- 카페 산정이 브랜드 2번


-- ══════════════════════════════════════════════
-- 3. users 정보 덮어쓰기
--    (회원가입 시 입력한 이름, 가게명 등을 더미값으로 교체)
-- ══════════════════════════════════════════════

UPDATE users SET
  NAME               = '카페디아즈',
  nickname           = '카페디아즈',
  contact_phone      = '01012341234',
  company_name       = '카페 디아즈',
  business_category  = '카페 / 베이커리',
  preferred_channel  = 'INSTAGRAM',
  store_phone_number = '0423226868',
  road_addr_part1    = '대전 중구 은행동 157-2',
  addr_detail        = '1층',
  terms_agreed       = 1,
  privacy_agreed     = 1,
  location_agreed    = 1,
  marketing_consent  = 1,
  event_consent      = 1,
  signup_completed   = 1,
  STATUS             = 'ACTIVE'
WHERE id = @DIAZ_USER_ID;

UPDATE users SET
  NAME               = '카페산정',
  nickname           = '카페산정',
  contact_phone      = '01056785678',
  company_name       = '카페 산정',
  business_category  = '카페 / 베이커리',
  preferred_channel  = 'INSTAGRAM',
  store_phone_number = '0427241234',
  road_addr_part1    = '대전 유성구 산정로 112',
  addr_detail        = '1층',
  terms_agreed       = 1,
  privacy_agreed     = 1,
  location_agreed    = 1,
  marketing_consent  = 0,
  event_consent      = 0,
  signup_completed   = 1,
  STATUS             = 'ACTIVE'
WHERE id = @SANJEONG_USER_ID;


-- ══════════════════════════════════════════════
-- 4. business_hours 재생성
-- ══════════════════════════════════════════════

-- 카페 디아즈: 화요일 정기휴무
INSERT INTO business_hours (user_id, day_of_week, is_open, open_time, close_time) VALUES
(@DIAZ_USER_ID, 0, 1, '09:00', '22:00'),
(@DIAZ_USER_ID, 1, 0,  NULL,    NULL  ),
(@DIAZ_USER_ID, 2, 1, '09:00', '22:00'),
(@DIAZ_USER_ID, 3, 1, '09:00', '22:00'),
(@DIAZ_USER_ID, 4, 1, '09:00', '22:00'),
(@DIAZ_USER_ID, 5, 1, '09:00', '22:00'),
(@DIAZ_USER_ID, 6, 1, '09:00', '22:00');

-- 카페 산정: 수요일 정기휴무
INSERT INTO business_hours (user_id, day_of_week, is_open, open_time, close_time) VALUES
(@SANJEONG_USER_ID, 0, 1, '10:00', '21:00'),
(@SANJEONG_USER_ID, 1, 1, '10:00', '21:00'),
(@SANJEONG_USER_ID, 2, 0,  NULL,    NULL  ),
(@SANJEONG_USER_ID, 3, 1, '10:00', '21:00'),
(@SANJEONG_USER_ID, 4, 1, '10:00', '21:00'),
(@SANJEONG_USER_ID, 5, 1, '10:00', '21:00'),
(@SANJEONG_USER_ID, 6, 1, '10:00', '21:00');


-- ══════════════════════════════════════════════
-- 5. content_settings 재생성
-- ══════════════════════════════════════════════

INSERT INTO content_settings
  (user_id, intro_template, outro_template, tone, emoji_level, target_length, preferred_sns)
VALUES
(@DIAZ_USER_ID,
 '안녕하세요, 대전 은행동 카페 디아즈입니다 ☕',
 '오늘도 디아즈에서 행복한 시간 보내세요 🌸',
 '친근한', '적당히', 300, 'instagram,kakao,naver');

INSERT INTO content_settings
  (user_id, intro_template, outro_template, tone, emoji_level, target_length, preferred_sns)
VALUES
(@SANJEONG_USER_ID,
 '안녕하세요, 대전 유성구 카페 산정입니다 :)',
 '방문해 주셔서 감사합니다. 또 오세요.',
 '기본', '적게', 250, 'instagram,naver');


-- ══════════════════════════════════════════════
-- 6. inquiry (마이페이지 문의 탭 확인용)
--    email 은 users 테이블에서 실제 값을 가져와서 INSERT
-- ══════════════════════════════════════════════

INSERT INTO inquiry (TYPE, email, title, BODY, content, STATUS, created_at)
SELECT '가입·연동', email,
  '인스타그램 연동 후 게시물 업로드가 되지 않아요',
  '인스타그램 연동은 완료됐는데 게시물 업로드 버튼을 누르면 아무 반응이 없습니다.',
  '인스타그램 연동은 완료됐는데 게시물 업로드 버튼을 누르면 아무 반응이 없습니다.',
  '처리완료', '2026-03-05 10:22:00'
FROM users WHERE id = @DIAZ_USER_ID;

INSERT INTO inquiry (TYPE, email, title, BODY, content, STATUS, created_at)
SELECT '콘텐츠 생성', email,
  'AI 콘텐츠 생성 시 업종이 반영이 안 됩니다',
  '카페 업종으로 설정했는데 생성된 내용이 식당 느낌으로 나옵니다.',
  '카페 업종으로 설정했는데 생성된 내용이 식당 느낌으로 나옵니다.',
  '처리중', '2026-03-18 14:35:00'
FROM users WHERE id = @DIAZ_USER_ID;

INSERT INTO inquiry (TYPE, email, title, BODY, content, STATUS, created_at)
SELECT '시스템오류', email,
  '채널 성과 분석 페이지가 로딩이 안 돼요',
  '채널 성과 분석 메뉴 클릭 시 흰 화면만 나타납니다. 다른 메뉴는 정상입니다.',
  '채널 성과 분석 메뉴 클릭 시 흰 화면만 나타납니다. 다른 메뉴는 정상입니다.',
  '미처리', '2026-03-28 09:11:00'
FROM users WHERE id = @DIAZ_USER_ID;

INSERT INTO inquiry (TYPE, email, title, BODY, content, STATUS, created_at)
SELECT '가입·연동', email,
  '네이버 블로그 연동이 계속 실패합니다',
  '네이버로 로그인 후 블로그 연동을 시도하면 "연동에 실패했습니다" 오류가 납니다.',
  '네이버로 로그인 후 블로그 연동을 시도하면 "연동에 실패했습니다" 오류가 납니다.',
  '미처리', '2026-03-12 11:40:00'
FROM users WHERE id = @SANJEONG_USER_ID;

INSERT INTO inquiry (TYPE, email, title, BODY, content, STATUS, created_at)
SELECT '계정', email,
  '가게 정보 수정 후 저장이 안 됩니다',
  '마이페이지에서 가게 주소를 수정하고 저장 버튼을 눌러도 변경이 안 됩니다.',
  '마이페이지에서 가게 주소를 수정하고 저장 버튼을 눌러도 변경이 안 됩니다.',
  '처리완료', '2026-03-20 16:05:00'
FROM users WHERE id = @SANJEONG_USER_ID;


SET FOREIGN_KEY_CHECKS = 1;


/*
  ── 실행 후 최종 확인 쿼리 ────────────────────────────────────────

  SELECT u.id, u.email, u.name, u.status,
         b.brand_id, b.brand_name
  FROM users u
  JOIN brand b ON b.user_id = u.id
  ORDER BY u.id;

  -- 기대 결과:
  -- id=A / accountA@gmail.com / 카페디아즈 / ACTIVE → brand_id=1 / 카페 디아즈
  -- id=B / accountB@gmail.com / 카페산정   / ACTIVE → brand_id=2 / 카페 산정

  ─────────────────────────────────────────────────────────────────
*/