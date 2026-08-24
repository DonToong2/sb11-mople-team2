package com.codeit.mople.domain.content.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.content.entity.ContentSortBy;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.search.ContentDocument;
import com.codeit.mople.domain.content.repository.search.ContentSearchRepository;
import com.codeit.mople.global.dto.SearchResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

// 단순 단위 테스트로 시도하고 싶었으나 H2에서는 Elasticsearch를 지원하지 않음
@SpringBootTest
@ActiveProfiles("test")
class ContentSearchRepositoryIntegrationTest {

  @Autowired
  private ContentSearchRepository contentSearchRepository;

  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  @BeforeEach
  void setUp() {
    contentSearchRepository.deleteAll();
    elasticsearchOperations.indexOps(ContentDocument.class).refresh();
  }

  @Test
  @DisplayName("콘텐츠 제목으로 검색 성공")
  void findByTitleContainingIgnoreCase_success() {
    // given
    UUID contentId1 = UUID.randomUUID();
    UUID contentId2 = UUID.randomUUID();
    UUID contentId3 = UUID.randomUUID();

    Instant now = Instant.now();

    contentSearchRepository.saveAll(List.of(
        new ContentDocument(
            contentId1,
            "새 콘텐츠 (1)",
            ContentType.MOVIE,
            4.5,
            100,
            now
        ),
        new ContentDocument(
            contentId2,
            "새 콘텐츠 (2)",
            ContentType.MOVIE,
            4.0,
            200,
            now
        ),
        new ContentDocument(
            contentId3,
            "새 콘텐츠 (3)",
            ContentType.TV_SERIES,
            3.5,
            300,
            now
        )
    ));

    elasticsearchOperations.indexOps(ContentDocument.class).refresh();

    // when
    SearchResult result = contentSearchRepository.findAllByTitleContainingIgnoreCase(
        "콘텐츠",
        null,
        null,
        10,
        null,
        ContentSortBy.CREATED_AT
    );

    // then
    assertThat(result.ids()).hasSize(3)
        .containsExactlyInAnyOrder(contentId1, contentId2, contentId3);
  }

  @Test
  @DisplayName("콘텐츠 제목을 대소문자 구분 없이 검색 성공")
  void findByTitleContainingIgnoreCase_ignoreCase() {
    // given
    UUID contentId = UUID.randomUUID();

    contentSearchRepository.save(
        new ContentDocument(
            contentId,
            "New Content (1)",
            ContentType.MOVIE,
            4.5,
            100,
            Instant.now()
        )
    );

    elasticsearchOperations.indexOps(ContentDocument.class).refresh();

    // when
    SearchResult result =
        contentSearchRepository.findAllByTitleContainingIgnoreCase(
            "nEw",
            null,
            null,
            10,
            null,
            ContentSortBy.CREATED_AT
        );

    // then
    assertThat(result.ids()).hasSize(1)
        .containsExactly(contentId);
  }

  @Test
  @DisplayName("검색 결과가 없으면 빈 목록 반환")
  void findByTitleContainingIgnoreCase_empty() {
    // given
    contentSearchRepository.save(
        new ContentDocument(
            UUID.randomUUID(),
            "새 콘텐츠 (1)",
            ContentType.MOVIE,
            4.5,
            100,
            Instant.now()
        )
    );

    elasticsearchOperations.indexOps(ContentDocument.class).refresh();

    // when
    SearchResult result =
        contentSearchRepository.findAllByTitleContainingIgnoreCase(
            "33",
            null,
            null,
            10,
            null,
            ContentSortBy.CREATED_AT
        );

    // then
    assertThat(result.ids()).isEmpty();
  }

}