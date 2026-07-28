package com.codeit.mople.domain.content.exception;

import com.codeit.mople.global.error.CustomException;

public class InvalidContentTypeException extends CustomException {
  public InvalidContentTypeException() {
    super(ContentErrorCode.INVALID_CONTENT_TYPE);
  }
}