package com.codeit.mople.domain.exception;

import com.codeit.mople.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContentErrorCode implements ErrorCode {
  INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "CONTENT-001", "지원하지 않는 콘텐츠 타입입니다."),
  INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "CONTENT-002", "조회 가능한 개수(limit)는 1 이상이어야 합니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
