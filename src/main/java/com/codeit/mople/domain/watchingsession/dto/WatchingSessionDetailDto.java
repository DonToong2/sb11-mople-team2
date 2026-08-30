package com.codeit.mople.domain.watchingsession.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDetailDto(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("createdAt")
    Instant createdAt,

    @JsonProperty("watcher")
    WatcherUserDto watcher,

    @JsonProperty("content")
    WatchingSessionContentDto content
) {

}