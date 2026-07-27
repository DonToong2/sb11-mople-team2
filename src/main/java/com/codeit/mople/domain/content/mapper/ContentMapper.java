package com.codeit.mople.domain.content.mapper;

import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.entity.Content;
import org.springframework.stereotype.Component;

@Component
public class ContentMapper {
  public ContentResponse toDto(Content content) {
    if (content == null) {
      return  null;
    }

    return new ContentResponse(
        content.getId(),
        content.getType().name(),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnailUrl(),
        content.getTags(),
        content.getAverageRating(),
        content.getReviewCount(),
        content.getWatcherCount()
    );
  }
}
