package com.codeit.mople.domain.watchingsession.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record WatcherUserDto(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("userId")
    UUID userId,

    @JsonProperty("name")
    String name,

    @JsonProperty("profileImageUrl")
    String profileImageUrl
) {
}