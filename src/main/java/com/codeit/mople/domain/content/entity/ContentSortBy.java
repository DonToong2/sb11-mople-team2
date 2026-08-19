package com.codeit.mople.domain.content.entity;

import com.codeit.mople.domain.content.exception.ContentErrorCode;
import com.codeit.mople.domain.content.exception.ContentException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public enum ContentSortBy {
  CREATED_AT("createdAt"),
  WATCHER_COUNT("watcherCount"),
  RATING("rating");

  private final String value;

  ContentSortBy(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  //입력된 정렬 키를 Enum으로 안전하게 변환
  public static ContentSortBy from(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) return CREATED_AT; // 기본값

    for (ContentSortBy type : values()) {
      if (type.value.equalsIgnoreCase(sortBy)) return type;
    }

    //별칭 처리
    if (List.of("ratingSum").contains(sortBy)) {
      return RATING;
    }

    throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("sortBy", sortBy));
  }

  public Object parseCursor(String cursorValue) {
    if (this == WATCHER_COUNT) {
      return Long.parseLong(cursorValue);
    } else if (this == RATING) {
      return Double.parseDouble(cursorValue);
    } else {
      return Instant.parse(cursorValue);
    }
  }

  public String extractCursorValue(Content content) {
    if (this == WATCHER_COUNT) {
      return String.valueOf(content.getWatcherCount());
    } else if (this == RATING) {
      return String.valueOf(content.getRatingSum());
    } else {
      return content.getCreatedAt() != null ? content.getCreatedAt().toString() : null;
    }
  }
}