package com.codeit.mople.domain.user.repository.search;

import com.codeit.mople.domain.user.dto.request.UserSortBy;
import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.global.dto.SearchResult;
import com.codeit.mople.global.dto.SortDirection;
import java.util.UUID;

public interface UserSearchRepositoryCustom {

  SearchResult findAllByEmailContainingIgnoreCase(
      String email,
      UUID cursorId,
      Object cursorValue,
      int limit,
      UserSortBy sortBy,
      SortDirection sortDirection,
      Role role,
      Boolean locked
  );
}
