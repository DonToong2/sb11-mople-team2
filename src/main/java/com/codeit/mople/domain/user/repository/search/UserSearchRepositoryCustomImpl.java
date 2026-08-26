package com.codeit.mople.domain.user.repository.search;

import com.codeit.mople.domain.user.dto.request.UserSortBy;
import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.global.dto.SearchResult;
import com.codeit.mople.global.dto.SortDirection;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSearchRepositoryCustomImpl
    implements UserSearchRepositoryCustom {

  private final ElasticsearchOperations elasticsearchOperations;

  @Override
  public SearchResult findAllByEmailContainingIgnoreCase(
      String email,
      UUID cursorId,
      Object cursorValue,
      int limit,
      UserSortBy sortBy,
      SortDirection sortDirection,
      Role role,
      Boolean locked
  ) {

    NativeQuery query = NativeQuery.builder()
        .withQuery(buildQuery(email, role, locked))
        .withPageable(PageRequest.of(0, limit + 1))
        .withSort(getSort(sortBy, sortDirection))
        .build();

    // 두 커서가 모두 존재할 때만 search_after 적용
    if (cursorId != null && cursorValue != null) {
      query.setSearchAfter(
          List.of(
              cursorValue instanceof Instant i ? i.toString() : cursorValue,
              cursorId.toString()
          )
      );
    }

    SearchHits<UserDocument> hits =
        elasticsearchOperations.search(
            query,
            UserDocument.class
        );

    List<SearchHit<UserDocument>> searchHits =
        hits.getSearchHits();

    boolean hasNext = searchHits.size() > limit;

    List<SearchHit<UserDocument>> pageHits =
        hasNext
            ? searchHits.subList(0, limit)
            : searchHits;

    if (pageHits.isEmpty()) {
      return new SearchResult(
          List.of(),
          null,
          null,
          false,
          hits.getTotalHits()
      );
    }

    List<UUID> ids = pageHits.stream()
        .map(SearchHit::getContent)
        .map(UserDocument::getId)
        .toList();

    UserDocument last =
        pageHits.get(pageHits.size() - 1).getContent();

    return new SearchResult(
        ids,
        extractCursor(last, sortBy),
        last.getId(),
        hasNext,
        hits.getTotalHits()
    );
  }

  private Query buildQuery(
      String email,
      Role role,
      Boolean locked
  ) {

    List<Query> filters = new ArrayList<>();

    if (role != null) {
      filters.add(
          Query.of(q -> q.term(
              t -> t
                  .field("role")
                  .value(role.name())
          ))
      );
    }

    if (locked != null) {
      filters.add(
          Query.of(q -> q.term(
              t -> t
                  .field("locked")
                  .value(locked)
          ))
      );
    }

    return Query.of(q -> q.bool(
        b -> {
          b.must(createEmailQuery(email));

          if (!filters.isEmpty()) {
            b.filter(filters);
          }

          return b;
        }
    ));
  }

  private Sort getSort(
      UserSortBy sortBy,
      SortDirection sortDirection
  ) {

    Sort.Direction direction =
        sortDirection == SortDirection.ASCENDING
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    return switch (sortBy) {

      case NAME -> Sort.by(
          new Sort.Order(direction, "name"),
          new Sort.Order(Sort.Direction.ASC, "id")
      );

      case EMAIL -> Sort.by(
          new Sort.Order(direction, "email.keyword"),
          new Sort.Order(Sort.Direction.ASC, "id")
      );

      case CREATED_AT -> Sort.by(
          new Sort.Order(direction, "createdAt"),
          new Sort.Order(Sort.Direction.ASC, "id")
      );

      case IS_LOCKED -> Sort.by(
          new Sort.Order(direction, "locked"),
          new Sort.Order(Sort.Direction.ASC, "id")
      );

      case ROLE -> Sort.by(
          new Sort.Order(direction, "role"),
          new Sort.Order(Sort.Direction.ASC, "id")
      );
    };
  }

  private String extractCursor(
      UserDocument user,
      UserSortBy sortBy
  ) {

    return switch (sortBy) {

      case NAME -> user.getName();

      case EMAIL -> user.getEmail();

      case CREATED_AT -> user.getCreatedAt().toString();

      case IS_LOCKED -> String.valueOf(user.getLocked());

      case ROLE -> user.getRole();
    };
  }

  // n-gram 범위를 벗어날 경우 fallback 처리
  private Query createEmailQuery(String email) {
    if (email.length() < 2 || email.length() > 10) {
      return Query.of(q -> q
          .wildcard(w -> w
              .field("email.keyword")
              .value("*" + email + "*") // 회원가입 시 이메일에 lowercase 적용시키기 때문에 toLowerCase로 호출X
          )
      );
    }

    return Query.of(q -> q
        .match(m -> m
            .field("email")
            .query(email)
        )
    );
  }
}