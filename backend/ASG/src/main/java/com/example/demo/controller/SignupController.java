package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;
import com.example.demo.dto.SignupRequest;
import com.example.demo.security.PrincipalDetails;
import com.example.demo.service.auth.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SignupController {

    private final UserService userService;
    
    @Value("${app.admin.email}")
    private String adminEmail;

    /**
     * signup/login 템플릿에서 공통으로 사용할 기본 모델값
     * - 기본은 "소셜 로그인 필요" 상태
     * - 로그인된 가입 미완료 사용자만 각 핸들러에서 false로 덮어씀
     */
    @ModelAttribute
    public void addDefaultAttributes(Model model) {
        model.addAttribute("socialLoginRequired", true);
    }

 // 변경 후
    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal PrincipalDetails principal,
                            Model model) {

        if (principal == null) {
            return "login";
        }

        Long userId = principal.getUser().getId();
        User user = userService.findById(userId);

        if (adminEmail.equals(user.getEmail())) {
            return "redirect:/admin";
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            return "redirect:/mypage";
        }

        return "login";
    }

    @GetMapping("/signup")
    public String signupPage(@AuthenticationPrincipal PrincipalDetails principal,
                             Model model) {

        log.info("[GET] /signup 진입 - principal={}", principal != null ? "존재" : "없음");

        if (principal == null) {
            model.addAttribute("socialLoginRequired", true);
            log.info("[GET] /signup - 비로그인 상태, 소셜 로그인 유도 화면");
            return "signup";
        }

        Long userId = principal.getUser().getId();
        User user = userService.findById(userId);

        log.info("[GET] /signup - DB 조회 결과 userId={}, email={}, status={}, signupCompleted={}",
                user.getId(), user.getEmail(), user.getStatus(), user.isSignupCompleted());

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.info("[GET] /signup - 이미 가입 완료된 사용자, /mypage 로 리다이렉트");
            return "redirect:/mypage";
        }

        model.addAttribute("socialLoginRequired", false);
        model.addAttribute("email", user.getEmail());
        model.addAttribute("name", user.getName());
        model.addAttribute("provider", user.getProvider());

        log.info("[GET] /signup - 가입 미완료 사용자, 추가 정보 입력 화면");
        return "signup";
    }

    @PostMapping("/signup/complete")
    public String completeSignup(@ModelAttribute SignupRequest dto,
                                 @AuthenticationPrincipal PrincipalDetails principal,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {

        log.info("[POST] /signup/complete 진입");

        if (principal == null) {
            log.warn("[POST] /signup/complete - principal 없음, /login 으로 리다이렉트");
            return "redirect:/login";
        }

        Long userId = principal.getUser().getId();
        log.info("[POST] /signup/complete - principal userId={}", userId);

        log.info("[POST] /signup/complete - dto.nickname={}", dto.getNickname());
        log.info("[POST] /signup/complete - dto.contactPhone={}", dto.getContactPhone());
        log.info("[POST] /signup/complete - dto.companyName={}", dto.getCompanyName());
        log.info("[POST] /signup/complete - dto.businessCategory={}", dto.getBusinessCategory());
        log.info("[POST] /signup/complete - dto.preferredChannel={}", dto.getPreferredChannel());
        log.info("[POST] /signup/complete - dto.storePhoneNumber={}", dto.getStorePhoneNumber());
        log.info("[POST] /signup/complete - dto.roadAddrPart1={}", dto.getRoadAddrPart1());
        log.info("[POST] /signup/complete - dto.addrDetail={}", dto.getAddrDetail());
        log.info("[POST] /signup/complete - dto.locationName={}", dto.getLocationName());
        log.info("[POST] /signup/complete - dto.termsAgreed={}", dto.isTermsAgreed());
        log.info("[POST] /signup/complete - dto.privacyAgreed={}", dto.isPrivacyAgreed());
        log.info("[POST] /signup/complete - dto.locationAgreed={}", dto.isLocationAgreed());
        log.info("[POST] /signup/complete - dto.marketingConsent={}", dto.getMarketingConsent());
        log.info("[POST] /signup/complete - dto.eventConsent={}", dto.getEventConsent());
        log.info("[POST] /signup/complete - dto.businessHours size={}",
                dto.getBusinessHours() != null ? dto.getBusinessHours().size() : null);

        if (!dto.isTermsAgreed() || !dto.isPrivacyAgreed()) {
            log.warn("[POST] /signup/complete - 필수 약관 미동의, userId={}", userId);
            redirectAttributes.addFlashAttribute("error", "필수 약관에 동의해주세요.");
            return "redirect:/signup";
        }

        if (userService.isNicknameDuplicated(dto.getNickname())) {
            log.warn("[POST] /signup/complete - 닉네임 중복, userId={}, nickname={}", userId, dto.getNickname());
            redirectAttributes.addFlashAttribute("error", "이미 사용중인 닉네임입니다.");
            return "redirect:/signup";
        }

        try {
            log.info("[POST] /signup/complete - userService.completeSignup 호출 전, userId={}", userId);

            userService.completeSignup(userId, dto);
            session.setAttribute("userStatus", "ACTIVE");

            User updatedUser = userService.findById(userId);
            log.info("[POST] /signup/complete - 저장 후 조회 userId={}, status={}, signupCompleted={}, nickname={}, companyName={}",
                    updatedUser.getId(),
                    updatedUser.getStatus(),
                    updatedUser.isSignupCompleted(),
                    updatedUser.getNickname(),
                    updatedUser.getCompanyName());

            if (adminEmail.equals(updatedUser.getEmail())) {
                log.info("[POST] /signup/complete - 관리자 계정, /admin 으로 리다이렉트, userId={}", userId);
                return "redirect:/admin";
            }
            log.info("[POST] /signup/complete - 회원가입 완료, /mypage 로 리다이렉트, userId={}", userId);
            return "redirect:/mypage";

        } catch (Exception e) {
            log.error("[POST] /signup/complete - 회원가입 저장 중 예외 발생, userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "회원가입 저장 중 오류가 발생했습니다.");
            return "redirect:/signup";
        }
    }
}