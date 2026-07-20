package com.example.demo.service;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.dto.ContentInitResponse;
import com.example.demo.dto.ContentRequest;
import com.example.demo.dto.SnsResult;
import com.example.demo.entity.GeneratedContent;
import com.example.demo.entity.Keyword;
import com.example.demo.entity.SnsGuide;
import com.example.demo.entity.UserSetting;
import com.example.demo.repository.GeneratedContentRepository;
import com.example.demo.repository.KeywordRepository;
import com.example.demo.repository.SnsGuideRepository;
import com.example.demo.repository.UserSettingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentService {

	private final GeminiApiClient geminiClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final GeneratedContentRepository contentRepository;

	// 마이페이지 구현용 필드
	private final com.example.demo.repository.myPage.ContentPostRepository contentPostRepository;
	private final com.example.demo.repository.myPage.BrandPlatformRepository brandPlatformRepository;
	private final com.example.demo.repository.myPage.BrandRepository brandRepository; // 추가
	private static final Long BRAND_ID = 1L;
	
	private final KeywordRepository keywordRepository;
    private final SnsGuideRepository snsGuideRepository;
    private final UserSettingRepository userSettingRepository;
    private final Random random = new Random();
    
    public ContentInitResponse getInitialData(String industryCode, Long userId) {
        // 1. 키워드 조회: 요청된 industryCode로 먼저 조회합니다.
        List<Keyword> rawKeywords = keywordRepository.findByIndustryCode(industryCode);
        
        // Rationale: 만약 조회 결과가 없다면(해당하지 않는 업종), 마이그레이션한 'DEFAULT' 키워드 전체를 조회합니다.
        if (rawKeywords.isEmpty()) {
            rawKeywords = keywordRepository.findByIndustryCode("DEFAULT");
        }

        // 2. 가이드 조회 (기존 랜덤 로직 유지)
        List<SnsGuide> rawGuides = snsGuideRepository.findAll();

        // 3. 사용자 설정 조회 (기존 로직 유지)
        UserSetting setting = userSettingRepository.findByUserId(userId)
                .orElseGet(() -> UserSetting.builder()
                        .userId(userId)
                        .activePlatforms(Arrays.asList("instagram"))
                        .toneStyle("default")
                        .emojiLevel("mid")
                        .maxLength(150)
                        .build());

        // 4. DTO 변환 및 반환
        return ContentInitResponse.builder()
                .keywords(rawKeywords.stream()
                        .map(k -> ContentInitResponse.KeywordDto.builder()
                                .keywordName(k.getName())
                                .category(k.getCategory()).build())
                        .toList())
                .snsGuides(rawGuides.stream()
                        .map(this::mapToRandomSnsGuideDto)
                        .toList())
                .userSetting(ContentInitResponse.UserSettingDto.builder()
                        .activeSns(setting.getActivePlatforms())
                        .toneStyle(setting.getToneStyle())
                        .emojiLevel(setting.getEmojiLevel())
                        .maxLength(setting.getMaxLength()).build())
                .build();
    }
    
    
    private ContentInitResponse.SnsGuideDto mapToRandomSnsGuideDto(SnsGuide guide) {
        // Rationale: 데이터베이스 조회 결과가 비어있을 경우 예외를 방지하고 프론트엔드에 빈 값을 전달합니다.
        if (guide.getGuideDetails() == null || guide.getGuideDetails().isEmpty()) {
            return ContentInitResponse.SnsGuideDto.builder()
                    .platform(guide.getPlatform())
                    .guideContent("")
                    .bestTime("--:--")
                    .build();
        }

        // Rationale: 동일한 인덱스를 사용하여 1:1로 매핑된 객체를 한 번만 추출합니다.
        SnsGuide.GuideDetail selectedDetail = guide.getGuideDetails()
                .get(random.nextInt(guide.getGuideDetails().size()));

        return ContentInitResponse.SnsGuideDto.builder()
                .platform(guide.getPlatform())
                .guideContent(selectedDetail.getContent())
                .bestTime(selectedDetail.getBestTime())
                .build();
    }

	@Transactional
	public List<SnsResult> generateAllSnsContent(ContentRequest request) {

		String industry = "카페 / 베이커리, store name : 디아즈 카페";

		String prompt = String.format("Role: %s Marketer. Task: Promo post. Lang: Korean.\n"
				+ "Menu/Item: %s\nPlatforms: %s\nExtra: %s\nKeywords: %s\nTone: %s\nEmoji: %s\nMaxLen: %d chars.\n"
				+ "Rule: STRICT JSON Array ONLY. NO markdown. "
				+ "If Platforms is NAVER, ignore MaxLen and write a long text of 1,000 characters or more."
				+ "Create one JSON object for EACH platform listed in 'Platforms'. "
				+ "Format: [{ \"platform\": \"<PLATFORM_NAME>\", \"content\": \"...\", \"hashtags\": [\"#tag1\", \"#tag2\"] }]",
				industry, request.getMenuName(), request.getPlatforms(), request.getExtraInfo(), request.getKeywords(),
				request.getTones(), request.getEmojiLevel(), request.getMaxLength());

		String rawJsonContent = geminiClient.requestToGemini(prompt);
		System.out.println("원시프롬프트:"+rawJsonContent);
		List<SnsResult> results = parseAndEnrichResults(rawJsonContent, request.getImageUrl());

		for (SnsResult res : results) {
			GeneratedContent entity = GeneratedContent.builder().menuName(request.getMenuName())
					.platform(res.getPlatform()).content(res.getContent())
					.hashtags(res.getHashtags() != null ?
					String.join(",", res.getHashtags()) : "")
					.imageUrl(request.getImageUrl()).build();
			contentRepository.save(entity);
		}

		// 마이페이지 히스토리 저장
		saveGeneratedContents(request.getMenuName(), results, request.getUserId());

		return results;
	}

	private List<SnsResult> parseAndEnrichResults(String rawJsonContent, String requestImageUrl) {
		List<SnsResult> results = new ArrayList<>();

		try {
			String cleanJson = rawJsonContent.replace("```json", "").replace("```", "").trim();

			List<SnsResult> parsedList = objectMapper.readValue(cleanJson, new TypeReference<List<SnsResult>>() {
			});

			for (SnsResult item : parsedList) {
				item.setImageUrl(requestImageUrl);
				switch (item.getPlatform()) {
				case "instagram":
					item.setPlatformAbbr("ig");
					item.setPlatformName("Instagram");
					item.setColor("#E1306C");
					item.setGuideText("감성적인 문구와 해시태그를 활용하세요.");
					item.setBestTime("저녁 7~9시");
					break;
				case "facebook":
					item.setPlatformAbbr("fb");
					item.setPlatformName("Facebook");
					item.setColor("#1877F2");
					item.setGuideText("정보 전달 위주의 깔끔한 구성이 좋습니다.");
					item.setBestTime("점심 12~1시");
					break;
				case "naver":
					item.setPlatformAbbr("nv");
					item.setPlatformName("네이버 블로그");
					item.setColor("#03C75A");
					item.setGuideText("검색 유입을 위해 키워드를 자연스럽게 배치하세요.");
					item.setBestTime("오전 8~10시");
					break;
				case "kakao":
					item.setPlatformAbbr("kk");
					item.setPlatformName("카카오 채널");
					item.setColor("#F9C000");
					item.setGuideText("가독성이 좋은 짧은 문장과 명확한 혜택을 강조하세요.");
					item.setBestTime("오후 5~7시");
					break;
				}
				results.add(item);
			}
		} catch (Exception e) {
			System.err.println("AI JSON 파싱 중 오류 발생: " + e.getMessage());
		}

		return results;
	}

	// 마이페이지-컨텐츠 생성 내역
		private void saveGeneratedContents(String menuName, List<SnsResult> results, Long userId) {
			// userId → brandId 해석 (null이면 하드코딩 fallback)
			Long resolvedBrandId = BRAND_ID;
			if (userId != null) {
			resolvedBrandId = brandRepository.findByUser_Id(userId)
					.map(b -> b.getBrandId())
					.orElse(BRAND_ID);
		}

		List<com.example.demo.entity.myPage.BrandPlatform> brandPlatforms = brandPlatformRepository
				.findByBrand_BrandId(resolvedBrandId);

		Map<String, com.example.demo.entity.myPage.BrandPlatform> platformMap = brandPlatforms.stream()
				.filter(bp -> bp.getPlatform() != null).collect(java.util.stream.Collectors
						.toMap(bp -> bp.getPlatform().getPlatformCode(), bp -> bp, (a, b) -> a));

		for (SnsResult result : results) {
			com.example.demo.entity.myPage.BrandPlatform bp = platformMap.get(result.getPlatform());
			if (bp == null)
				continue;

			com.example.demo.entity.myPage.ContentPost post = new com.example.demo.entity.myPage.ContentPost();
			post.setBrandPlatform(bp);
			post.setPostTitle(menuName);
			post.setPostType("AI생성");
			post.setPostBody(result.getContent());
			post.setStatus("published");

			try {
				contentPostRepository.save(post);
			} catch (Exception e) {
				System.err.println("content_post 저장 실패 [" + result.getPlatform() + "]: " + e.getMessage());
			}
		}
	}
}