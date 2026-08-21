package com.codeit.mople.domain.watchingsession.dto;

import java.util.UUID;

public record WatchingSessionEvent(
    UUID contentId,
    WatchingSessionChange changeEvent
) {
}