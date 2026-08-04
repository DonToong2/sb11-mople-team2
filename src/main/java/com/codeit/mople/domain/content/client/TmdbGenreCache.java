package com.codeit.mople.domain.content.client;

import com.codeit.mople.domain.content.client.dto.TmdbGenreListResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// TMDB 장르는 고정 데이터라 메모리에 저장
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbGenreCache {

  private final TmdbClient tmdbClient;

  private volatile Map<Integer, String> genreNames = Map.of();

  @EventListener(ApplicationReadyEvent.class)
  public void loadOnStartup() {
    refresh();
  }

  public void refresh() {
    try {
      Map<Integer, String> loaded = new HashMap<>();
      putAll(loaded, tmdbClient.getMovieGenres());
      putAll(loaded, tmdbClient.getTvGenres());
      genreNames = Map.copyOf(loaded);
      log.info("TMDB 장르 캐시 적재 완료: {}건", genreNames.size());
    } catch (Exception e) {
      log.warn("TMDB 장르 캐시 적재 실패, 다음 조회 시 재시도한다.", e);
    }
  }

  public List<String> getNames(List<Integer> genreIds) {
    if (genreIds == null || genreIds.isEmpty()) {
      return List.of();
    }
    Map<Integer, String> current = currentGenres();
    return genreIds.stream()
        .map(id -> resolve(current, id))
        .filter(Objects::nonNull)
        .toList();
  }

  public String getName(Integer genreId) {
    return resolve(currentGenres(), genreId);
  }

  private Map<Integer, String> currentGenres() {
    if (genreNames.isEmpty()) {
      refresh();
    }
    return genreNames;
  }

  private String resolve(Map<Integer, String> genres, Integer genreId) {
    String name = genres.get(genreId);
    if (name == null) {
      log.warn("알 수 없는 TMDB 장르 id: {}", genreId);
    }
    return name;
  }

  private void putAll(Map<Integer, String> target, TmdbGenreListResponse response) {
    if (response == null || response.genres() == null) {
      return;
    }
    response.genres().forEach(genre -> target.put(genre.id(), genre.name()));
  }
}
