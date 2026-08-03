package com.codeit.mople.domain.watchingsession.dto;

import java.util.UUID;

public record WatcherDto(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
