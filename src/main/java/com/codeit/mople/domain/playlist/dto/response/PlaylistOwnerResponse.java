package com.codeit.mople.domain.playlist.dto.response;

import java.util.UUID;

public record PlaylistOwnerResponse(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
