package com.codeit.mople.domain.watchingsession.dto;

import java.time.Instant;
import java.util.UUID;

public record WatchingSessionResponse(
    UUID id,
    Instant createdAt,
    WatcherDto watcher,
    WatchingSessionContentDto content
) {

}
