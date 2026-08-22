package com.codeit.mople.domain.user.exception;

import com.codeit.mople.global.error.CustomException;
import java.util.Map;

public class UserException extends CustomException {

  public UserException(UserErrorCode errorCode) {
    super(errorCode);
  }

  public UserException(UserErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
