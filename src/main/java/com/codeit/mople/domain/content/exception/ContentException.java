package com.codeit.mople.domain.content.exception;

import com.codeit.mople.global.error.CustomException;
import java.util.Map;

public class ContentException extends CustomException {

  public ContentException(ContentErrorCode errorCode) {
    super(errorCode);
  }

  public ContentException(ContentErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
