package com.codeit.mople.domain.review.exception;

import com.codeit.mople.global.error.CustomException;
import java.util.Map;

public class ReviewException extends CustomException {

  public ReviewException(ReviewErrorCode errorCode) {
    super(errorCode);
  }

  public ReviewException(ReviewErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
