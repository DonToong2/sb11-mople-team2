package com.codeit.mople.domain.watchingsession.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WatchingSessionChange(
    @JsonProperty("type")
    String type,

    @JsonProperty("watchingSession")
    WatchingSessionDetailDto watchingSession,

    @JsonProperty("watcherCount")
    Integer watcherCount
) {

}