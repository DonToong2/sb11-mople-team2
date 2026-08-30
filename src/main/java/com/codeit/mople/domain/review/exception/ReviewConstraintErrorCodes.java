package com.codeit.mople.domain.review.exception;

import com.codeit.mople.global.error.ConstraintErrorCodes;
import com.codeit.mople.global.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReviewConstraintErrorCodes implements ConstraintErrorCodes {

  @Override
  public Map<String, ErrorCode> get() {
    return Map.of(
        "uk_review_content_author", ReviewErrorCode.REVIEW_ALREADY_EXISTS
    );
  }
}
