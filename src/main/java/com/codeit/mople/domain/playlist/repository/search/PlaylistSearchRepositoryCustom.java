package com.codeit.mople.domain.playlist.repository.search;

import com.codeit.mople.domain.playlist.dto.request.PlaylistQueryCondition.PlaylistSortBy;
import com.codeit.mople.global.dto.SearchResult;
import com.codeit.mople.global.dto.SortDirection;
import java.util.UUID;

public interface PlaylistSearchRepositoryCustom {

  SearchResult findAllByTitleContainingIgnoreCase(
      String title,
      UUID cursorId,
      Object cursorValue,
      int limit,
      PlaylistSortBy sortBy,
      SortDirection sortDirection
  );
}