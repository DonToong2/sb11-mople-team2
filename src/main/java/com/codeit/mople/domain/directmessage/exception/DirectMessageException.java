package com.codeit.mople.domain.directmessage.exception;

import com.codeit.mople.global.error.CustomException;
import java.util.Map;

public class DirectMessageException extends CustomException {

  public DirectMessageException(DirectMessageErrorCode errorCode) {
    super(errorCode);
  }

  public DirectMessageException(DirectMessageErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
