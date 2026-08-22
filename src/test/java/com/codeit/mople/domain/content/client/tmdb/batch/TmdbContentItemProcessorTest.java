package com.codeit.mople.domain.content.client.tmdb.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.content.client.tmdb.config.TmdbProperties;
import com.codeit.mople.domain.content.client.tmdb.dto.TmdbContentItem;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TmdbContentItemProcessor 단위 테스트")
class TmdbContentItemProcessorTest {

  private static final ContentType TYPE = ContentType.MOVIE;
  private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
  private static final int ACTION = 28;
  private static final int ADVENTURE = 12;

  private TmdbContentItemProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new TmdbContentItemProcessor(
        TYPE,
        Map.of(ACTION, "액션", ADVENTURE, "모험"),
        new TmdbProperties("test-key", IMAGE_BASE_URL)
    );
  }

  private record Item(
      Long id,
      String contentTitle,
      String overview,
      String posterPath,
      List<Integer> genreIds
  ) implements TmdbContentItem {

  }

  @Nested
  @DisplayName("Content 변환")
  class Convert {

    @Test
    @DisplayName("변환에 성공하면 TMDB 항목의 값을 Content에 담고 외부 id를 채움")
    void processSuccess() throws Exception {
      // given
      Item item = new Item(550L, "파이트 클럽", "줄거리", "/poster.jpg", List.of(ACTION, ADVENTURE));

      // when
      Content content = processor.process(item);

      // then
      assertThat(content).isNotNull();
      assertThat(content.getType()).isEqualTo(TYPE);
      assertThat(content.getTitle()).isEqualTo("파이트 클럽");
      assertThat(content.getDescription()).isEqualTo("줄거리");
      assertThat(content.getThumbnailUrl()).isEqualTo(IMAGE_BASE_URL + "/poster.jpg");
      assertThat(content.getTags()).containsExactly("액션", "모험");
      assertThat(content.getExternalId()).isEqualTo("550");
    }

    @Test
    @DisplayName("포스터 경로가 없으면 썸네일을 채우지 않음")
    void processSuccessWhenPosterPathIsNull() throws Exception {
      // given
      Item item = new Item(550L, "파이트 클럽", "줄거리", null, List.of(ACTION));

      // when
      Content content = processor.process(item);

      // then
      assertThat(content).isNotNull();
      assertThat(content.getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("장르 이름을 모르는 id는 태그에서 제외")
    void processSuccessWhenGenreIdIsUnknown() throws Exception {
      // given
      Item item = new Item(550L, "파이트 클럽", "줄거리", "/poster.jpg", List.of(ACTION, 9999));

      // when
      Content content = processor.process(item);

      // then
      assertThat(content).isNotNull();
      assertThat(content.getTags()).containsExactly("액션");
    }

    @Test
    @DisplayName("장르 id가 없으면 빈 태그 목록을 반환")
    void processSuccessWhenGenreIdsAreEmpty() throws Exception {
      // given
      Item item = new Item(550L, "파이트 클럽", "줄거리", "/poster.jpg", List.of());

      // when
      Content content = processor.process(item);

      // then
      assertThat(content).isNotNull();
      assertThat(content.getTags()).isEmpty();
    }
  }

  @Nested
  @DisplayName("항목 필터링")
  class Filter {

    @Test
    @DisplayName("제목이 없으면 null을 반환")
    void processFilterWhenTitleIsNull() throws Exception {
      // given
      Item item = new Item(550L, null, "줄거리", "/poster.jpg", List.of(ACTION));

      // when
      Content content = processor.process(item);

      // then
      assertThat(content).isNull();
    }

    @Test
    @DisplayName("제목이 공백뿐이면 null을 반환")
    void processFilterWhenTitleIsBlank() throws Exception {
      // given
      Item item = new Item(550L, "   ", "줄거리", "/poster.jpg", List.of(ACTION));

      // when
      Content content = processor.process(item);

      // then
      assertThat(content).isNull();
    }

    @Test
    @DisplayName("id가 없으면 null을 반환")
    void processFilterWhenIdIsNull() throws Exception {
      // given
      Item item = new Item(null, "파이트 클럽", "줄거리", "/poster.jpg", List.of(ACTION));

      // when
      Content content = processor.process(item);

      // then
      assertThat(content).isNull();
    }
  }
}