package com.codeit.mople.domain.auth.security;

import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.jwt.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
    this.jwtProvider = jwtProvider;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    String token = resolveToken(request);

    if(token != null) {
      try {
        UUID userId = jwtProvider.getUserId(token);
        long tokenSessionVersion = jwtProvider.getSessionVersion(token);

        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isPresent() && userOpt.get().getSessionVersion() == tokenSessionVersion) {
          User user = userOpt.get();
          CustomUserDetails principal = new CustomUserDetails(user.getId(), user.getRole());
          var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // sessionVersion이 다르면 = 다른 기기에서 재로그인함 -> 인증 안 세팅, 401로 자연스럽게 이어짐
      } catch (JwtException | IllegalArgumentException e) {
        // 토큰이 유효하지 않으면 인증 세팅 안 함 -> 401
      }
    }
    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearer = request.getHeader("Authorization");
    if(bearer != null && bearer.startsWith("Bearer ")) {
      return bearer.substring(7);
    }
    return null;
  }
}
