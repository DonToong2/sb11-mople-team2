package com.codeit.mople.domain.auth.exception;

import com.codeit.mople.global.error.CustomException;
import java.util.Map;

public class AuthException extends CustomException {

  public AuthException(AuthErrorCode errorCode) {
    super(errorCode);
  }

  public AuthException(AuthErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
