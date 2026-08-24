package com.codeit.mople.domain.content.repository.search;

import com.codeit.mople.domain.content.entity.ContentSortBy;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.global.dto.SearchResult;
import java.util.UUID;

public interface ContentSearchRepositoryCustom {
  SearchResult findAllByTitleContainingIgnoreCase(
      String title,
      UUID cursorId,
      Object cursorValue,
      int limit,
      ContentType type,
      ContentSortBy sortBy
  );
}
