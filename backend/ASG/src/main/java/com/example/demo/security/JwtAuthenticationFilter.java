package com.example.demo.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.repository.auth.UserRepository;
import com.example.demo.security.jwt.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.Cookie;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

    	// 변경 후
    	String token = null;

    	String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    	if (authHeader != null && authHeader.startsWith("Bearer ")) {
    	    token = authHeader.substring(7);
    	} else {
    	    Cookie[] cookies = request.getCookies();
    	    if (cookies != null) {
    	        for (Cookie cookie : cookies) {
    	            if ("access_token".equals(cookie.getName())) {
    	                token = cookie.getValue();
    	                break;
    	            }
    	        }
    	    }
    	}

    	if (token != null && jwtTokenProvider.validate(token) && "access".equals(jwtTokenProvider.getTokenType(token))) {
    	    Long userId = jwtTokenProvider.getUserId(token);

    	    userRepository.findById(userId).ifPresent(user -> {
    	        PrincipalDetails principalDetails = new PrincipalDetails(user);

    	        UsernamePasswordAuthenticationToken authentication =
    	                new UsernamePasswordAuthenticationToken(
    	                        principalDetails,
    	                        null,
    	                        principalDetails.getAuthorities()
    	                );

    	        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    	        SecurityContextHolder.getContext().setAuthentication(authentication);
    	    });
    	}

        filterChain.doFilter(request, response);
    }
}