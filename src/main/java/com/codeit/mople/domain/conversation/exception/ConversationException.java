package com.codeit.mople.domain.conversation.exception;

import com.codeit.mople.global.error.CustomException;
import java.util.Map;

public class ConversationException extends CustomException {

  public ConversationException(ConversationErrorCode errorCode) {
    super(errorCode);
  }

  public ConversationException(ConversationErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
