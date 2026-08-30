package com.codeit.mople.domain.content.exception;

import com.codeit.mople.global.error.CustomException;
import java.util.UUID;

public class ContentNotFoundException extends CustomException {

  private ContentNotFoundException(UUID contentId) {
    super(ContentErrorCode.CONTENT_NOT_FOUND);
  }

  public static ContentNotFoundException withId(UUID contentId) {
    return new ContentNotFoundException(contentId);
  }
}