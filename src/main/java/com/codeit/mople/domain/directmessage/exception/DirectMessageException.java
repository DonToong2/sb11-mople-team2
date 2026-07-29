package com.codeit.mople.domain.directmessage.exception;

import com.codeit.mople.global.error.CustomException;
import com.codeit.mople.global.error.ErrorCode;
import java.util.Map;

public class DirectMessageException extends CustomException {

  public DirectMessageException(ErrorCode errorCode) {
    super(errorCode);
  }

  public DirectMessageException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
