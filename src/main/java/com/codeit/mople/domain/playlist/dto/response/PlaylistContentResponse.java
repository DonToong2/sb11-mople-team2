package com.codeit.mople.domain.playlist.dto.response;

import java.util.List;
import java.util.UUID;

public record PlaylistContentResponse(
    UUID id,
    String type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    double averageRating,
    int reviewCount
) {

}
