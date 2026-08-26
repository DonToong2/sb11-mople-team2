package com.codeit.mople.domain.playlist.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.codeit.mople.domain.playlist.dto.request.PlaylistQueryCondition.PlaylistSortBy;
import com.codeit.mople.global.dto.SearchResult;
import com.codeit.mople.global.dto.SortDirection;
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
public class PlaylistSearchRepositoryCustomImpl
    implements PlaylistSearchRepositoryCustom {

  private final ElasticsearchOperations elasticsearchOperations;

  @Override
  public SearchResult findAllByTitleContainingIgnoreCase(
      String title,
      UUID cursorId,
      Object cursorValue,
      int limit,
      PlaylistSortBy sortBy,
      SortDirection sortDirection
  ) {

    NativeQuery query = NativeQuery.builder()
        .withQuery(createTitleQuery(title))
        .withPageable(PageRequest.of(0, limit + 1))
        .withSort(getSort(sortBy, sortDirection))
        .build();

    if (cursorId != null && cursorValue != null) {
      query.setSearchAfter(
          List.of(cursorValue, cursorId.toString())
      );
    }

    SearchHits<PlaylistDocument> hits =
        elasticsearchOperations.search(
            query,
            PlaylistDocument.class
        );

    List<SearchHit<PlaylistDocument>> searchHits =
        hits.getSearchHits();

    boolean hasNext = searchHits.size() > limit;

    List<SearchHit<PlaylistDocument>> pageHits =
        hasNext
            ? searchHits.subList(0, limit)
            : searchHits;

    if (pageHits.isEmpty()) {
      return new SearchResult(
          List.of(),
          null,
          null,
          false,
          0
      );
    }

    List<UUID> ids = pageHits.stream()
        .map(SearchHit::getContent)
        .map(PlaylistDocument::getId)
        .toList();

    PlaylistDocument last =
        pageHits.get(pageHits.size() - 1).getContent();

    return new SearchResult(
        ids,
        extractCursor(last, sortBy),
        last.getId(),
        hasNext,
        count(title)
    );
  }

  private Sort getSort(
      PlaylistSortBy sortBy,
      SortDirection sortDirection
  ) {
    Sort.Direction direction =
        sortDirection == SortDirection.ASCENDING
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    return switch (sortBy) {
      case UPDATED_AT -> Sort.by(
          new Sort.Order(direction, "updatedAt"),
          new Sort.Order(Sort.Direction.ASC, "id.keyword")
      );

      case SUBSCRIBE_COUNT -> Sort.by(
          new Sort.Order(direction, "subscribeCount"),
          new Sort.Order(Sort.Direction.ASC, "id.keyword")
      );
    };
  }

  private String extractCursor(
      PlaylistDocument playlist,
      PlaylistSortBy sortBy
  ) {
    return switch (sortBy) {
      case UPDATED_AT -> playlist.getUpdatedAt().toString();

      case SUBSCRIBE_COUNT -> String.valueOf(playlist.getSubscribeCount());
    };
  }

  private long count(String title) {
    NativeQuery query = NativeQuery.builder()
        .withQuery(createTitleQuery(title))
        .withPageable(PageRequest.of(0, 1))
        .build();

    SearchHits<PlaylistDocument> hits =
        elasticsearchOperations.search(
            query,
            PlaylistDocument.class
        );

    return hits.getTotalHits();
  }

  // n-gram 범위를 벗어날 경우 fallback 처리
  private Query createTitleQuery(String title) {
    if (title.length() < 2 || title.length() > 10) {
      return Query.of(q -> q
          .wildcard(w -> w
              .field("title.keyword")
              .value("*" + title.toLowerCase() + "*")
          )
      );
    }

    return Query.of(q -> q
        .match(m -> m
            .field("title")
            .query(title)
        )
    );
  }
}