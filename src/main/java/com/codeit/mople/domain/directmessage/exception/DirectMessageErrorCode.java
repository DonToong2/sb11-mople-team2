package com.codeit.mople.domain.directmessage.exception;

import com.codeit.mople.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DirectMessageErrorCode implements ErrorCode {

  DIRECT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "DM-001", "쪽지 찾을 수 없습니다."),
  UNAUTHORIZED_RECEIVER(HttpStatus.FORBIDDEN, "DM-002", "해당 쪽지를 읽음 처리할 권한이 없습니다."),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "DM-003", "입력값이 유효하지 않습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
