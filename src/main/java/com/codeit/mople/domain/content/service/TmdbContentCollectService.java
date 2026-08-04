package com.codeit.mople.domain.content.service;

import com.codeit.mople.domain.content.client.TmdbClient;
import com.codeit.mople.domain.content.client.TmdbGenreCache;
import com.codeit.mople.domain.content.client.TmdbProperties;
import com.codeit.mople.domain.content.client.dto.TmdbMovieResponse;
import com.codeit.mople.domain.content.client.dto.TmdbPageResponse;
import com.codeit.mople.domain.content.client.dto.TmdbTvResponse;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.exception.ContentErrorCode;
import com.codeit.mople.domain.content.exception.ContentException;
import com.codeit.mople.domain.content.repository.ContentRepository;
import feign.RetryableException;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TmdbContentCollectService {

  private final TmdbClient tmdbClient;
  private final TmdbGenreCache tmdbGenreCache;
  private final TmdbProperties tmdbProperties;
  private final ContentRepository contentRepository;

  @Transactional
  public int collectMovies(int page) {
    TmdbPageResponse<TmdbMovieResponse> response =
        callTmdb(() -> tmdbClient.getPopularMovies(page));

    List<Content> contents = response.results().stream()
        .map(this::toContent)
        .filter(this::isNew)
        .toList();
    return saveAll(contents, ContentType.movie, page);
  }

  @Transactional
  public int collectTvSeries(int page) {
    TmdbPageResponse<TmdbTvResponse> response =
        callTmdb(() -> tmdbClient.getPopularTvSeries(page));

    List<Content> contents = response.results().stream()
        .map(this::toContent)
        .filter(this::isNew)
        .toList();

    return saveAll(contents, ContentType.tvSeries, page);
  }

  private <T> T callTmdb(Supplier<T> call) {
    try {
      return call.get();
    } catch (RetryableException e) {
      log.error("TMDB 재시도 소진: {}", e.getMessage());
      throw new ContentException(ContentErrorCode.TMDB_TEMPORARILY_UNAVAILABLE);
    }
  }

  private Content toContent(TmdbMovieResponse movie) {
    return new Content(
        ContentType.movie,
        movie.title(),
        movie.overview(),
        toThumbnailUrl(movie.posterPath()),
        tmdbGenreCache.getNames(movie.genreIds()));
  }

  private Content toContent(TmdbTvResponse tv) {
    return new Content(
        ContentType.tvSeries,
        tv.name(),
        tv.overview(),
        toThumbnailUrl(tv.posterPath()),
        tmdbGenreCache.getNames(tv.genreIds()));
  }

  private String toThumbnailUrl(String posterPath) {
    return posterPath == null ? null : tmdbProperties.imageBaseUrl() + posterPath;
  }

  private boolean isNew(Content content) {
    return !contentRepository.existsByTypeAndTitle(content.getType(), content.getTitle());
  }

  private int saveAll(List<Content> contents, ContentType type, int page) {
    if (contents.isEmpty()) {
      log.info("TMDB 수집 결과 없음: type={}, page={}", type, page);
      return 0;
    }
    List<Content> saved = contentRepository.saveAll(contents);
    log.info("TMDB 수집 완료: type={}, page={}, 저장={}건", type, page, saved.size());
    return saved.size();
  }

}
