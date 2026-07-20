package com.example.demo.service.myPage;

import com.example.demo.dto.myPage.BrandInfoRequest;
import com.example.demo.dto.myPage.BrandInfoResponse;
import com.example.demo.dto.myPage.ContentSettingsRequest;
import com.example.demo.dto.myPage.ContentSettingsResponse;
import com.example.demo.dto.myPage.SnsAccountResponse;
import com.example.demo.entity.myPage.Brand;
import com.example.demo.entity.myPage.BrandOperationProfile;
import com.example.demo.entity.myPage.BrandPlatform;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.domain.ContentSettings;
import com.example.demo.domain.enums.IndustryType;
import com.example.demo.repository.myPage.BrandOperationProfileRepository;
import com.example.demo.repository.myPage.BrandPlatformRepository;
import com.example.demo.repository.myPage.BrandRepository;
import com.example.demo.repository.myPage.ContentPostRepository;
import com.example.demo.repository.myPage.ContentSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.domain.user.entity.BusinessHours;
import com.example.demo.repository.auth.BusinessHoursRepository;
import java.util.List;
import java.util.Map;
import com.example.demo.dto.myPage.ContentSettingsResponse;
import com.example.demo.dto.myPage.ContentSettingsRequest;

import com.example.demo.domain.user.entity.User;
import com.example.demo.repository.auth.UserRepository;

import java.io.IOException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MypageService {

	private final BrandRepository brandRepository;
	private final UserRepository userRepository;
	private final BrandOperationProfileRepository operationProfileRepository;
	private final BrandPlatformRepository brandPlatformRepository;
	private final ContentSettingsRepository contentSettingsRepository;
	private final Cloudinary cloudinary;
	private final BusinessHoursRepository businessHoursRepository;
	private final com.example.demo.repository.myPage.InquiryRepository inquiryRepository;
	private final ContentPostRepository contentPostRepository;

	// ── 가게 정보 조회 ──────────────────────────────────────
	@Transactional(readOnly = true)
	public BrandInfoResponse getBrandInfo(Long userId) {
	    // 1. orElse(null)을 사용하여 예외 발생을 막음
	    Brand brand = brandRepository.findByUser_Id(userId).orElse(null);

	    // 2. 브랜드가 없으면 에러가 아니라 null을 담은 응답 객체 혹은 null 자체를 반환
	    if (brand == null) {
	        return null; 
	        // 또는 return new BrandInfoResponse(null, null); (프론트와 협의 필요)
	    }

	    BrandOperationProfile profile = operationProfileRepository.findByBrand_BrandId(brand.getBrandId()).orElse(null);

	    if (brand.getAddress() != null) {
	        brand.setAddress(stripZipCode(brand.getAddress()));
	    }
	    return new BrandInfoResponse(brand, profile);
	}
	
	// ── 회원 기본정보 수정 ──────────────────────────────────
	@Transactional
	public void updateMemberInfo(Long userId, String name, String contactPhone) {
	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
	    user.updateSocialInfo(user.getEmail(), name);
	    user.updateContactPhone(contactPhone);
	}

	// ── 가게 정보 수정 ──────────────────────────────────────
	@Transactional
	public void updateBrandInfo(Long userId, BrandInfoRequest request) {
		Brand brand = brandRepository.findByUser_Id(userId)
				.orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));

		brand.setBrandName(request.getBrandName());
		brand.setServiceName(request.getServiceName());
		brand.setIndustryType(IndustryType.fromDescription(request.getIndustryType()));

		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
		// ❌ 제거: user.updateAddress(request.getAddress(), request.getLocationName());
		user.updateStorePhone(request.getPhone());
		user.updateCompanyInfo(request.getBrandName(), request.getIndustryType());
		user.updateAddress(request.getAddress(), request.getAddrDetail());  // ✅ 추가
		brand.setAddress(stripZipCode(request.getAddress()));
		brand.setLocationName(request.getLocationName());

		BrandOperationProfile profile = operationProfileRepository.findByBrand_BrandId(brand.getBrandId())
				.orElse(new BrandOperationProfile());
		profile.setBrand(brand);
		profile.setOpenTime(request.getOpenTime());
		profile.setCloseTime(request.getCloseTime());
		profile.setRegularClosedWeekday(request.getRegularClosedWeekday());
		operationProfileRepository.save(profile);
	}

	// ── 우편번호 제거 헬퍼 ──────────────────────────────────
	private String stripZipCode(String address) {
		if (address == null)
			return null;
		return address.replaceAll("^\\[\\d+\\]\\s*", "").trim();
	}

	// ── 콘텐츠 설정 조회 ────────────────────────────────────
	@Transactional(readOnly = true)
	public ContentSettingsResponse getContentSettings(Long userId) {
		return contentSettingsRepository.findByUser_Id(userId).map(ContentSettingsResponse::new).orElse(null);
	}

	// ── 콘텐츠 설정 수정 ────────────────────────────────────
	@Transactional
	public void updateContentSettings(Long userId, ContentSettingsRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

		ContentSettings settings = contentSettingsRepository.findByUser_Id(userId)
				.orElse(ContentSettings.createDefault(user));

		settings.update(request.getIntroTemplate(), request.getOutroTemplate(), request.getTone(),
		        request.getEmojiLevel(), request.getTargetLength(), request.getPreferredSns(),
		        request.getUseDefaultMode());  // ✅ 추가
		contentSettingsRepository.save(settings);
	}

	// ── SNS 연동 목록 조회 ──────────────────────────────────
	@Transactional(readOnly = true)
	public List<SnsAccountResponse> getSnsAccounts(Long userId) {
		Brand brand = brandRepository.findByUser_Id(userId)
				.orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));

		// DB에 연동된 플랫폼 목록
		List<BrandPlatform> connected = brandPlatformRepository.findByBrand_BrandId(brand.getBrandId());

		// platformCode 기준으로 Map 변환
		Map<String, BrandPlatform> connectedMap = connected.stream()
				.collect(Collectors.toMap(bp -> bp.getPlatform().getPlatformCode(), bp -> bp));

		// 4개 플랫폼 고정 목록
		List<String[]> fixedPlatforms = List.of(new String[] { "instagram", "Instagram" },
				new String[] { "facebook", "Facebook" }, new String[] { "naver", "네이버 블로그" },
				new String[] { "kakao", "카카오채널" });

		return fixedPlatforms.stream().map(p -> {
			String code = p[0];
			String name = p[1];
			BrandPlatform bp = connectedMap.get(code);
			return bp != null ? new SnsAccountResponse(bp) // 연동 정보 있음
					: SnsAccountResponse.notConnected(code, name); // 미연동
		}).collect(Collectors.toList());
	}

	// ── SNS 연동 해제 ───────────────────────────────────────
	@Transactional
	public void disconnectSns(Long brandPlatformId) {
		BrandPlatform bp = brandPlatformRepository.findById(brandPlatformId)
				.orElseThrow(() -> new IllegalArgumentException("연동 정보를 찾을 수 없습니다."));

		bp.setIsConnected(false);
		bp.setAccessToken(null);
		bp.setRefreshToken(null);
		bp.setTokenStatus("EXPIRED");
	}

	// ── 회원 정보 조회 ──────────────────────────────────────
	@Transactional(readOnly = true)
	public User getUserInfo(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
	}

	// ── 회원 탈퇴 ──────────────────────────────────────
	@Transactional
	public void withdrawUser(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
		user.withdraw();
	}

	// ── 대표이미지 ──────────────────────────────────────
	@Transactional
	public String uploadProfileImage(Long userId, MultipartFile file) throws IOException {
		Brand brand = brandRepository.findByUser_Id(userId)
				.orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));

		Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
				ObjectUtils.asMap("public_id", "brand_" + brand.getBrandId()));
		String imageUrl = (String) uploadResult.get("secure_url");
		brand.setProfileImageUrl(imageUrl);
		return imageUrl;
	}

	// ── 주소 업데이트 ──────────────────────────────────────
	@Transactional
	public void updateAddress(Long userId, String roadAddrPart1, String addrDetail) {
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
		user.updateAddress(roadAddrPart1, addrDetail);
	}

	/**
	 * 영업시간 저장 (UPSERT 방식: 기존 7행 삭제 후 재삽입) user_id + day_of_week UNIQUE 제약 조건을 활용하는
	 * replaceBusinessHours 위임
	 *
	 * @param hours 프론트에서 전달된 7개 요일 데이터 [{dayOfWeek:0, isOpen:true,
	 *              openTime:"09:00", closeTime:"22:00"}, ...]
	 */

	@Transactional
	public void updateBusinessHours(Long userId, List<Map<String, Object>> hours) {
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

		businessHoursRepository.deleteAllByUserId(userId);
		businessHoursRepository.flush(); // DELETE 먼저 확정

		List<BusinessHours> newHours = hours.stream().map(h -> {
			int day = toInt(h.get("dayOfWeek"));
			boolean open = toBoolean(h.get("isOpen"));
			String openT = open ? toString(h.get("openTime")) : null;
			String closeT = open ? toString(h.get("closeTime")) : null;
			return open ? BusinessHours.openDay(user, day, openT, closeT) : BusinessHours.closedDay(user, day);
		}).toList();

		businessHoursRepository.saveAll(newHours);
	}

	// ── 타입 변환 헬퍼 ───────────────────────────────────────
	private int toInt(Object v) {
		if (v instanceof Integer i)
			return i;
		if (v instanceof Number n)
			return n.intValue();
		return Integer.parseInt(v.toString());
	}

	private boolean toBoolean(Object v) {
		if (v instanceof Boolean b)
			return b;
		return "true".equalsIgnoreCase(v.toString());
	}

	private String toString(Object v) {
		return v == null ? null : v.toString();
	}

	// ── 영업시간 조회 ──────────────────────────────────────────
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getBusinessHours(Long userId) {
		return businessHoursRepository.findByUser_IdOrderByDayOfWeekAsc(userId).stream().map(bh -> {
			Map<String, Object> m = new java.util.LinkedHashMap<>();
			m.put("dayOfWeek", bh.getDayOfWeek());
			m.put("isOpen", bh.isOpen()); // getIsOpen() ❌ → isOpen() ✅
			m.put("openTime", bh.getOpenTime() != null ? bh.getOpenTime() : "09:00");
			m.put("closeTime", bh.getCloseTime() != null ? bh.getCloseTime() : "22:00");
			return m;
		}).toList();
	}

	// ── 문의 내역 조회 ──────────────────────────────────────
	@Transactional(readOnly = true)
	public List<com.example.demo.dto.myPage.InquiryResponse> getInquiries(String email) {
		return inquiryRepository.findByEmailOrderByCreatedAtDesc(email).stream()
				.map(com.example.demo.dto.myPage.InquiryResponse::new).collect(Collectors.toList());
	}

	// ── 콘텐츠 히스토리 조회 ────────────────────────────────
	@Transactional(readOnly = true)
	public List<ContentHistoryResponse> getContentHistory(Long userId) {
		System.out.println("=== [히스토리 조회] userId=" + userId); // 임시 디버그
		Brand brand = brandRepository.findByUser_Id(userId)
				.orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));
		System.out.println("=== [히스토리 조회] brandId=" + brand.getBrandId()); // 임시 디버그
		return contentPostRepository.findByBrandIdOrderByPublishedAtDesc(brand.getBrandId()).stream()
				.map(ContentHistoryResponse::new).collect(Collectors.toList());
	}

	// ── 히스토리 응답 DTO (inner record) ───────────────────
	public record ContentHistoryResponse(Long postId, String platformCode, String platformName, String postTitle,
			String postBody, java.time.LocalDateTime publishedAt) {
		public ContentHistoryResponse(com.example.demo.entity.myPage.ContentPost cp) {
			this(cp.getPostId(), cp.getBrandPlatform().getPlatform().getPlatformCode(),
					cp.getBrandPlatform().getPlatform().getPlatformName(), cp.getPostTitle(), cp.getPostBody(),
					cp.getPublishedAt());
		}
	}
	
	// ── userId → brandId 조회 (채널 성과 분석 등 타 컨트롤러에서도 사용) ──
	@Transactional(readOnly = true)
	public Long getBrandId(Long userId) {
	    return brandRepository.findByUser_Id(userId)
	            .map(Brand::getBrandId)
	            .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));
	}

}
