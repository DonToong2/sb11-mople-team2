package com.codeit.mople.domain.playlist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.playlist.dto.request.PlaylistQueryCondition.PlaylistSortBy;
import com.codeit.mople.domain.playlist.repository.search.PlaylistDocument;
import com.codeit.mople.domain.playlist.repository.search.PlaylistSearchRepository;
import com.codeit.mople.global.dto.SearchResult;
import com.codeit.mople.global.dto.SortDirection;
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
class PlaylistSearchRepositoryIntegrationTest {

  @Autowired
  private PlaylistSearchRepository playlistSearchRepository;

  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  @BeforeEach
  void setUp() {
    playlistSearchRepository.deleteAll();
    elasticsearchOperations.indexOps(PlaylistDocument.class).refresh();
  }

  @Test
  @DisplayName("플레이리스트 제목으로 검색 성공")
  void findByTitleContainingIgnoreCase_success() {
    // given
    UUID playlistId1 = UUID.randomUUID();
    UUID playlistId2 = UUID.randomUUID();
    UUID playlistId3 = UUID.randomUUID();

    Instant now = Instant.now();

    playlistSearchRepository.saveAll(List.of(
        new PlaylistDocument(playlistId1, "새 플레이리스트 (1)", 10, now),
        new PlaylistDocument(playlistId2, "새 플레이리스트 (2)", 20, now),
        new PlaylistDocument(playlistId3, "I am", 30, now)
    ));

    elasticsearchOperations.indexOps(PlaylistDocument.class).refresh();

    // when
    SearchResult result = playlistSearchRepository.findAllByTitleContainingIgnoreCase(
        "플레이",
        null,
        null,
        10,
        PlaylistSortBy.UPDATED_AT,
        SortDirection.ASCENDING
    );

    // then
    assertThat(result.ids()).hasSize(2)
        .containsExactlyInAnyOrder(playlistId1, playlistId2);
  }

  @Test
  @DisplayName("플레이리스트 제목을 대소문자 구분 없이 검색 성공")
  void findByTitleContainingIgnoreCase_ignoreCase() {
    // given
    UUID playlistId = UUID.randomUUID();

    playlistSearchRepository.save(
        new PlaylistDocument(
            playlistId,
            "New Playlist (1)",
            10,
            Instant.now()
        )
    );

    elasticsearchOperations.indexOps(PlaylistDocument.class).refresh();

    // when
    SearchResult result = playlistSearchRepository.findAllByTitleContainingIgnoreCase(
        "nEw",
        null,
        null,
        10,
        PlaylistSortBy.UPDATED_AT,
        SortDirection.ASCENDING
    );

    // then
    assertThat(result.ids()).hasSize(1)
        .containsExactly(playlistId);
  }

  @Test
  @DisplayName("검색 결과가 없으면 빈 목록 반환")
  void findByTitleContainingIgnoreCase_empty() {
    // given
    playlistSearchRepository.save(
        new PlaylistDocument(
            UUID.randomUUID(),
            "새 플레이리스트 (1)",
            10,
            Instant.now()
        )
    );

    elasticsearchOperations.indexOps(PlaylistDocument.class).refresh();

    // when
    SearchResult result = playlistSearchRepository.findAllByTitleContainingIgnoreCase(
        "33",
        null,
        null,
        10,
        PlaylistSortBy.UPDATED_AT,
        SortDirection.ASCENDING
    );

    // then
    assertThat(result.ids()).isEmpty();
  }

}