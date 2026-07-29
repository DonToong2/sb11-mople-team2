package com.codeit.mople.domain.playlist.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistResponse(
    UUID id,
    PlaylistOwnerResponse owner,
    String title,
    String description,
    Instant updatedAt,
    long subscriberCount,
    boolean subscribedByMe,
    List<PlaylistContentResponse> contents
) {

}
