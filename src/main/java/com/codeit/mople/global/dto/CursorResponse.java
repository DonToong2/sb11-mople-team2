package com.codeit.mople.global.dto;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record CursorResponse<T>(
    List<T> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {
  public static <T> CursorResponse<T> of(
      List<T> fetched,
      int limit,
      String sortBy,
      String sortDirection,
      Function<T, String> cursorExtractor,
      Function<T, UUID> idExtractor
  ) {
    boolean hasNext = fetched.size() > limit;
    List<T> data = hasNext ? fetched.subList(0, limit) : fetched;

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext && !data.isEmpty()) {
      T last = data.get(data.size() - 1);
      nextCursor = cursorExtractor.apply(last);
      nextIdAfter = idExtractor.apply(last);
    }

    // totalCount는 현재 반환 데이터 개수 또는 전체 개수 처리
    return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, data.size(), sortBy, sortDirection);
  }
}