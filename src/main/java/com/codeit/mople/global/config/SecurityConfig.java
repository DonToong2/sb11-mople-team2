package com.codeit.mople.global.config;

import com.codeit.mople.domain.auth.security.JwtAuthenticationFilter;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * TODO: JWT 인증/인가 완성 전까지의 임시 설정
 *
 * 현재 상태:
 * - 공개 엔드포인트(회원가입, Swagger)만 permitAll
 * - 그 외 모든 요청은 인증 필요 (JWT 필터가 아직 없어 실제로는 401 처리됨)
 *   → 사용자 조회/변경 API는 JWT 완성 전까지 사실상 비활성화 상태
 *
 * 완성 후 반영할 것:
 * - JWT 기반 인증 필터 등록
 * - X-User-Id 헤더 기반 요청자 식별을 @AuthenticationPrincipal로 전환
 * - CSRF 토큰을 쿠키(XSRF-TOKEN)로 관리, 헤더(X-XSRF-TOKEN)로 검증
 * - [어드민] 전용 API에 role 기반 접근 제어(hasRole("ADMIN")) 추가
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, JwtProvider jwtProvider,
      UserRepository userRepository) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // 쿠키명 기본값 XSRF-TOKEN, 헤더명 X-XSRF-TOKEN
            .ignoringRequestMatchers("/api/auth/**", "/api/users")
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(((request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/users/*/locked").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtProvider, userRepository),
            UsernamePasswordAuthenticationFilter.class
        );
    return http.build();
  }
}
