package com.codeit.mople.domain.conversation.exception;

import com.codeit.mople.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConversationErrorCode implements ErrorCode {

  CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CONVERSATION-001", "대화방을 찾을 수 없습니다."),
  INVALID_PARTICIPANT(HttpStatus.BAD_REQUEST, "CONVERSATION-002", "자기 자신과는 대화방을 생성할 수 없습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "CONVERSATION-003", "해당 대화방에 접근할 권한이 없습니다.")
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
