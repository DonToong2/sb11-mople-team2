package com.codeit.mople.domain.auth.controller;

import com.codeit.mople.domain.auth.dto.request.SignInRequest;
import com.codeit.mople.domain.auth.dto.response.TokenResponse;
import com.codeit.mople.domain.auth.service.AuthService;
import com.codeit.mople.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증 관리")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/sign-in")
  @Operation(summary = "로그인", description = "SecurityFilterChain에서 처리합니다.")
  public ApiResponse<TokenResponse> signIn(@Valid @RequestBody SignInRequest request) {
    return ApiResponse.success(authService.signIn(request));
  }

  @PostMapping("/sign-out")
  @Operation(summary = "로그아웃", description = "SecurityFilterChain에서 처리합니다.")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void signOut() {
    // 서버 측 별도 처리 없음. 클라이언트가 토큰은 폐기하는 것으로 로그아웃 완료.
    // (sessionVersion은 재로그인 시 강제 로그아웃을 위해 sign-in 시점에만 증가)
  }

  @GetMapping("/csrf-token")
  @Operation(summary = "CSRF 토큰 조회", description = "CSRF 토큰을 조회합니다. 토큰은 쿠키(XSRF-TOKEN)에 저장됩니다.")
  public ApiResponse<Void> csrfToken(CsrfToken csrfToken) {
    csrfToken.getToken();
    return ApiResponse.success();
  }
}
