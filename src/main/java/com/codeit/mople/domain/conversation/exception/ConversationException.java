package com.codeit.mople.domain.conversation.exception;

import com.codeit.mople.global.error.CustomException;
import com.codeit.mople.global.error.ErrorCode;
import java.util.Map;

public class ConversationException extends CustomException {

  public ConversationException(ErrorCode errorCode) {
    super(errorCode);
  }

  public ConversationException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
