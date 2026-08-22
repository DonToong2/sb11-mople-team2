package com.codeit.mople.domain.content.client.tmdb.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TMDB 응답 역직렬화 테스트")
class TmdbResponseDeserializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private <T> TmdbPageResponse<T> readPage(String fixture, Class<T> itemType) throws Exception {
    JavaType type = objectMapper.getTypeFactory()
        .constructParametricType(TmdbPageResponse.class, itemType);

    try (InputStream json = getClass().getClassLoader().getResourceAsStream(fixture)) {
      assertThat(json).isNotNull();
      return objectMapper.readValue(json, type);
    }
  }

  @Nested
  @DisplayName("영화 목록 응답")
  class MoviePage {

    @Test
    @DisplayName("페이지 정보를 스네이크 케이스 키에서 읽어옴")
    void readPageInfo() throws Exception {
      // when
      TmdbPageResponse<TmdbMovieResponse> page =
          readPage("tmdb/movie-popular.json", TmdbMovieResponse.class);

      // then
      assertThat(page.page()).isEqualTo(1);
      assertThat(page.totalPages()).isEqualTo(58251);
      assertThat(page.totalResults()).isEqualTo(1165007);
      assertThat(page.results()).hasSize(20);
    }

    @Test
    @DisplayName("영화 항목의 값을 채우고 모르는 필드는 무시")
    void readMovieItem() throws Exception {
      // when
      TmdbPageResponse<TmdbMovieResponse> page =
          readPage("tmdb/movie-popular.json", TmdbMovieResponse.class);
      TmdbMovieResponse movie = page.results().get(0);

      // then
      assertThat(movie.id()).isEqualTo(969681L);
      assertThat(movie.title()).isEqualTo("스파이더맨: 브랜드 뉴 데이");
      assertThat(movie.overview()).isNotBlank();
      assertThat(movie.posterPath()).isEqualTo("/8mLepBa5l591xFidRpn65xV7hb4.jpg");
      assertThat(movie.genreIds()).containsExactly(878, 28, 12);
    }

    @Test
    @DisplayName("영화는 title을 제목으로 반환")
    void readMovieContentTitle() throws Exception {
      // when
      TmdbPageResponse<TmdbMovieResponse> page =
          readPage("tmdb/movie-popular.json", TmdbMovieResponse.class);
      TmdbMovieResponse movie = page.results().get(0);

      // then
      assertThat(movie.contentTitle()).isEqualTo(movie.title());
    }
  }

  @Nested
  @DisplayName("TV 목록 응답")
  class TvPage {

    @Test
    @DisplayName("TV 항목의 값을 채우고 모르는 필드는 무시")
    void readTvItem() throws Exception {
      // when
      TmdbPageResponse<TmdbTvResponse> page =
          readPage("tmdb/tv-popular.json", TmdbTvResponse.class);
      TmdbTvResponse tv = page.results().get(0);

      // then
      assertThat(tv.id()).isEqualTo(5920L);
      assertThat(tv.name()).isEqualTo("멘탈리스트");
      assertThat(tv.overview()).isNotBlank();
      assertThat(tv.posterPath()).isEqualTo("/82hq7b8rPG0TFTV4SJk8Zfxj24H.jpg");
      assertThat(tv.genreIds()).containsExactly(80, 18, 9648);
    }

    @Test
    @DisplayName("TV는 original_name이 아니라 name을 제목으로 반환")
    void readTvContentTitle() throws Exception {
      // when
      TmdbPageResponse<TmdbTvResponse> page =
          readPage("tmdb/tv-popular.json", TmdbTvResponse.class);
      TmdbTvResponse tv = page.results().get(0);

      // then
      assertThat(tv.contentTitle()).isEqualTo("멘탈리스트");
    }
  }

  @Nested
  @DisplayName("장르 목록 응답")
  class GenreList {

    @Test
    @DisplayName("장르 id와 이름을 채움")
    void readGenres() throws Exception {
      // when
      TmdbGenreListResponse response;
      try (InputStream json = getClass().getClassLoader()
          .getResourceAsStream("tmdb/genre-movie-list.json")) {
        assertThat(json).isNotNull();
        response = objectMapper.readValue(json, TmdbGenreListResponse.class);
      }

      // then
      List<TmdbGenreListResponse.Genre> genres = response.genres();
      assertThat(genres).hasSize(19);
      assertThat(genres.get(0).id()).isEqualTo(28);
      assertThat(genres.get(0).name()).isEqualTo("액션");
    }
  }
}