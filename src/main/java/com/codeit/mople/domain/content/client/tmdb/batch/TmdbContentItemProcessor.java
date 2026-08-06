package com.codeit.mople.domain.content.client.tmdb.batch;

import com.codeit.mople.domain.content.client.tmdb.TmdbGenreCache;
import com.codeit.mople.domain.content.client.tmdb.config.TmdbProperties;
import com.codeit.mople.domain.content.client.tmdb.dto.TmdbContentItem;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
public class TmdbContentItemProcessor implements ItemProcessor<TmdbContentItem, Content> {

  private final ContentType contentType;
  private final TmdbGenreCache genreCache;
  private final TmdbProperties properties;

  // tmdb가 준 데이터를 Content로 바꿈
  @Override
  public @Nullable Content process(@NonNull TmdbContentItem item) throws Exception {
    String title = item.contentTitle();

    if (title == null || title.isBlank()) {
      log.warn("제목이 없는 TMDB 항목 필터링: id={}", item.id());
      return null;
    }

    // 도메인 + 이미지이름
    String thumbnailUrl = resolveThumbnailUrl(item.posterPath());
    List<String> tags = genreCache.getNames(item.genreIds());

    return new Content(contentType, title, item.overview(), thumbnailUrl, tags);
  }

  private String resolveThumbnailUrl(String posterPath) {
    return posterPath == null ? null : properties.imageBaseUrl() + posterPath;
  }
}
