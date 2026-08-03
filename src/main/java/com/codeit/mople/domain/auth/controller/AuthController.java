package com.codeit.mople.domain.auth.controller;

import com.codeit.mople.domain.auth.dto.request.ResetPasswordRequest;
import com.codeit.mople.domain.auth.dto.request.SignInRequest;
import com.codeit.mople.domain.auth.dto.response.AuthTokens;
import com.codeit.mople.domain.auth.dto.response.TokenResponse;
import com.codeit.mople.domain.auth.exception.AuthErrorCode;
import com.codeit.mople.domain.auth.exception.AuthException;
import com.codeit.mople.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

  private final AuthService authService;

  @Value("${cookie.secure:true}")
  private boolean cookieSecure;

  @PostMapping("/sign-in")
  public ResponseEntity<TokenResponse> signIn(@Valid @ModelAttribute SignInRequest request) {
    AuthTokens tokens = authService.signIn(request);
    return withRefreshTokenCookie(tokens);
  }

  @PostMapping("/sign-out")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> signOut() {
    // 서버 측 별도 처리 없음. 클라이언트가 토큰은 폐기하는 것으로 로그아웃 완료.
    // (sessionVersion은 재로그인 시 강제 로그아웃을 위해 sign-in 시점에만 증가)
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie().toString())
        .build();
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(
      @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
    if(refreshToken == null) {
      throw new AuthException(AuthErrorCode.INVALID_TOKEN);
    }
    AuthTokens tokens = authService.refresh(refreshToken);
    return withRefreshTokenCookie(tokens);
  }

  @GetMapping("/csrf-token")
  public ResponseEntity<Void> csrfToken(CsrfToken csrfToken) {
    csrfToken.getToken();
    return ResponseEntity.ok().build();
  }

  private ResponseEntity<TokenResponse> withRefreshTokenCookie(AuthTokens tokens) {
    ResponseCookie cookie = buildRefreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiresAt());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new TokenResponse(tokens.accessToken(), tokens.userDto()));
  }

  private ResponseCookie buildRefreshTokenCookie(String refreshToken, Instant expiresAt) {
    long maxAgeSeconds = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(maxAgeSeconds)
        .build();
  }

  private ResponseCookie expireRefreshTokenCookie() {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(0)
        .build();
  }
}
