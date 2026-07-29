package com.codeit.mople.domain.auth.exception;

import com.codeit.mople.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-002", "유효하지 않은 토큰입니다."),
  EXPIRED_SESSION(HttpStatus.UNAUTHORIZED, "AUTH-003", "다른 기기에서 로그인하여 세션이 만료되었습니다."),
  LOCKED_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH-004", "잠긴 계정입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
