package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.domain.user.entity.User;
import com.example.demo.dto.AiImageRequestDto;
import com.example.demo.dto.AiImageResponseDto;
import com.example.demo.security.PrincipalDetails;
import com.example.demo.service.AiImageService;
import com.example.demo.service.auth.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiImageController {
    private final AiImageService aiImageService;
    private final UserService userService;

    @PostMapping("/generate")
    public ResponseEntity<AiImageResponseDto> generateAiImage(
            @RequestBody AiImageRequestDto request,
            Authentication authentication) {

        if (authentication == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        String email = null;

        // 1. JWT 필터를 통해 인증된 경우 (현재 상황)
        if (authentication.getPrincipal() instanceof PrincipalDetails principalDetails) {
            // PrincipalDetails 내부에 있는 User 엔티티나 이메일 필드에서 직접 가져옵니다.
            email = principalDetails.getUser().getEmail(); 
        } 
        // 2. 만약 OAuth2AuthenticationToken으로 들어오는 소셜 로그인인 경우
        else if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            email = (String) attributes.get("email");
            
            // 카카오 등 특수 케이스 처리 (필요시)
            if (email == null && attributes.containsKey("kakao_account")) {
                Map<String, Object> kakao = (Map<String, Object>) attributes.get("kakao_account");
                email = (String) kakao.get("email");
            }
        }

        // 최종 방어막: 위에서 모두 실패하면 getName() 시도
        if (email == null) {
            email = authentication.getName();
        }

        log.info("최종 추출된 이메일: {}", email);

        // 이제 "이선주"가 아닌 "test@example.com"으로 조회하게 됩니다.
        User user = userService.findByEmail(email);

        String category = user.getBusinessCategory();
        if (category == null || category.isBlank()) {
            throw new IllegalStateException("업종 정보가 설정되지 않았습니다. 프로필을 먼저 완성해주세요.");
        }

        log.info("AI 이미지 생성 진행 - email={}, category={}", email, category);

        AiImageResponseDto response = aiImageService.generateImage(
            request.getKeyword(),
            category,
            request.getDescription()
        );

        return ResponseEntity.ok(response);
    }
}