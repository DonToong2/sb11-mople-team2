package com.codeit.mople.domain.user.dto.request;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.global.dto.SortDirection;
import java.util.UUID;

public record UserSearchRequest(
    String emailLike,
    Role roleEqual,
    Boolean isLocked,
    String cursor,
    UUID idAfter,
    Integer limit,
    SortDirection sortDirection,
    UserSortBy sortBy
) {
  public int limitOrDefault() {
    return (limit == null || limit <= 0) ? 20 : limit;
  }

  public SortDirection sortDirectionOrDefault() {
    return sortDirection == null ? SortDirection.ASCENDING : sortDirection;
  }

  public UserSortBy sortByOrDefault() {
    return sortBy == null ? UserSortBy.name : sortBy;
  }
}
