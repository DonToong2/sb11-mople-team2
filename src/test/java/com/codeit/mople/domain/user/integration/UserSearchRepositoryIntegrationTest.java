package com.codeit.mople.domain.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.user.dto.request.UserSortBy;
import com.codeit.mople.domain.user.repository.search.UserDocument;
import com.codeit.mople.domain.user.repository.search.UserSearchRepository;
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
class UserSearchRepositoryIntegrationTest {

  @Autowired
  private UserSearchRepository userSearchRepository;

  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  @BeforeEach
  void setUp() {
    userSearchRepository.deleteAll();
    elasticsearchOperations.indexOps(UserDocument.class).refresh();
  }

  @Test
  @DisplayName("이메일로 사용자 검색 성공")
  void findByEmailContainingIgnoreCase_success() {
    // given
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    UUID userId3 = UUID.randomUUID();

    Instant now = Instant.now();

    userSearchRepository.saveAll(List.of(
        new UserDocument(userId1, "user1@test.com", "user1", now, false, "USER"),
        new UserDocument(userId2, "user2@test.com", "user2", now, false, "USER"),
        new UserDocument(userId3, "admin@test.com", "admin", now, false, "ADMIN")
    ));

    elasticsearchOperations.indexOps(UserDocument.class).refresh();

    // when
    SearchResult result = userSearchRepository.findAllByEmailContainingIgnoreCase(
        "test",
        null,
        null,
        10,
        UserSortBy.CREATED_AT,
        SortDirection.ASCENDING,
        null,
        null
    );

    // then
    assertThat(result.ids()).hasSize(3)
        .containsExactlyInAnyOrder(userId1, userId2, userId3);
  }

  @Test
  @DisplayName("검색 결과가 없으면 빈 목록 반환")
  void findByEmailContainingIgnoreCase_empty() {
    // given
    userSearchRepository.save(
        new UserDocument(
            UUID.randomUUID(),
            "user@test.com",
            "user",
            Instant.now(),
            false,
            "USER"
        )
    );

    elasticsearchOperations.indexOps(UserDocument.class).refresh();

    // when
    SearchResult result = userSearchRepository.findAllByEmailContainingIgnoreCase(
        "what",
        null,
        null,
        10,
        UserSortBy.CREATED_AT,
        SortDirection.ASCENDING,
        null,
        null
    );

    // then
    assertThat(result.ids()).isEmpty();
  }
}