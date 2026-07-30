package com.codeit.mople.global.config;

import com.codeit.mople.domain.auth.security.JwtAuthenticationFilter;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, JwtProvider jwtProvider,
      UserRepository userRepository) throws Exception {
    http
        //JWT 기반의 Stateless API이므로 CSRF 보호 비활성화
        .csrf(csrf -> csrf.disable())

        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(((request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
            .requestMatchers("/", "/index.html", "/favicon.svg", "/assets/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/users/*/locked").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/contents/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/contents/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/contents/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtProvider, userRepository),
            UsernamePasswordAuthenticationFilter.class
        );
    return http.build();
  }
}
