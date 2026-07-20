package com.example.demo.domain.user.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.demo.domain.ContentSettings;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = { "provider", "provider_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 소셜 로그인 ──────────────────────────────────────────
    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String email;

    @Column(length = 50)
    private String name;

    // ── 프로필 ───────────────────────────────────────────────
    @Column(length = 50)
    private String nickname;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    // ── 사업자 정보 ──────────────────────────────────────────
    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "business_category", length = 50)
    private String businessCategory;

    @Column(name = "preferred_channel", length = 20)
    private String preferredChannel;

    @Column(name = "store_phone_number", length = 20)
    private String storePhoneNumber;

    // ── 주소 필드 ──
    @Column(name = "road_addr_part1", length = 200)
    private String roadAddrPart1;   // 도로명주소 본체

    @Column(name = "addr_detail", length = 100)
    private String addrDetail;      // 사용자 직접 입력 상세주소

    // ── 약관 동의 ────────────────────────────────────────────
    @Column(name = "terms_agreed", nullable = false)
    @Builder.Default
    private boolean termsAgreed = false;

    @Column(name = "privacy_agreed", nullable = false)
    @Builder.Default
    private boolean privacyAgreed = false;

    @Column(name = "location_agreed", nullable = false)
    @Builder.Default
    private boolean locationAgreed = false;

    @Column(name = "marketing_consent")
    @Builder.Default
    private Boolean marketingConsent = false;

    @Column(name = "event_consent")
    @Builder.Default
    private Boolean eventConsent = false;

    // ── 회원 상태 ────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.SIGNUP_PENDING;

    @Column(name = "signup_completed", nullable = false, columnDefinition = "TINYINT")
    @Builder.Default
    private boolean signupCompleted = false;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_USER";

    // ── 연관관계 ─────────────────────────────────────────────
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC")
    @Builder.Default
    private List<BusinessHours> businessHours = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private ContentSettings contentSettings;

    // ── 타임스탬프 ───────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void completeSignup(
            String nickname, String contactPhone,
            String companyName, String businessCategory,
            String preferredChannel, String storePhoneNumber,
            boolean termsAgreed, boolean privacyAgreed, boolean locationAgreed,
            Boolean marketingConsent, Boolean eventConsent
    ) {
    	// 유저 정보
        this.nickname = nickname;
        this.contactPhone = contactPhone;
        this.companyName = companyName;
        this.businessCategory = businessCategory;
        this.preferredChannel = preferredChannel;
        this.storePhoneNumber = storePhoneNumber;
        // 약관
        this.termsAgreed = termsAgreed;
        this.privacyAgreed = privacyAgreed;
        this.locationAgreed = locationAgreed;
        this.marketingConsent = marketingConsent;
        this.eventConsent = eventConsent;
        this.status = UserStatus.ACTIVE;
        this.signupCompleted = true;
    }

    public void updateSocialInfo(String email, String name) {
        this.email = email;
        this.name = name;
    }

    /**
     * 주소 업데이트 (우편번호, 도로명주소, 상세주소만 관리)
     */
    public void updateAddress(String roadAddrPart1, String addrDetail) {
        this.roadAddrPart1 = roadAddrPart1;  // 도로명주소 본체
        this.addrDetail = addrDetail;    // 사용자 직접 입력 상세주소
    }

    /**
     * 영업시간 일괄 교체
     */
    public void replaceBusinessHours(List<BusinessHours> newHours) {
        this.businessHours.clear(); // 여기서 기존 데이터가 삭제됩니다.
        if (newHours != null) {
            this.businessHours.addAll(newHours);
        }
    }

    /**
     * AI 콘텐츠 설정 저장/교체
     */
    public void applyContentSettings(ContentSettings settings) {
        this.contentSettings = settings;
    }
    
    // 탈퇴 관련 메서드
    public void withdraw() {
        this.status = UserStatus.INACTIVE;
    }
    
    // 가게 대표 전화번호 업데이트
    public void updateStorePhone(String storePhoneNumber) {
        this.storePhoneNumber = storePhoneNumber;
    }

    // 가게명·업종 업데이트
    public void updateCompanyInfo(String companyName, String businessCategory) {
        this.companyName = companyName;
        this.businessCategory = businessCategory;
    }
    
    public static User createSocialUser(String provider, String providerId,
                                         String email, String name) {
        return User.builder()
            .provider(provider)
            .providerId(providerId)
            .email(email)
            .name(name)
            .status(UserStatus.SIGNUP_PENDING)
            .signupCompleted(false)
            .role("ROLE_USER")
            .termsAgreed(false)
            .privacyAgreed(false)
            .locationAgreed(false)
            .marketingConsent(false)
            .eventConsent(false)
            .build();
    }
    
 // 연락처 업데이트
    public void updateContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    // 관리자 계정 업데이트
    public void updateRole(String role) {
        this.role = role;
    }
}