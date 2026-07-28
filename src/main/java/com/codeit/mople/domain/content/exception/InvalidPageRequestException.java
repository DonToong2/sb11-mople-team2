package com.codeit.mople.domain.content.exception;

import com.codeit.mople.global.error.CustomException;

public class InvalidPageRequestException extends CustomException {
  public InvalidPageRequestException() {
    super(ContentErrorCode.INVALID_PAGE_REQUEST);
  }
}