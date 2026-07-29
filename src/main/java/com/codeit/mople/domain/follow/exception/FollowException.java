package com.codeit.mople.domain.follow.exception;

import com.codeit.mople.global.error.CustomException;
import com.codeit.mople.global.error.ErrorCode;
import java.util.Map;

public class FollowException extends CustomException {

  public FollowException(ErrorCode errorCode) {
    super(errorCode);
  }

  public FollowException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
