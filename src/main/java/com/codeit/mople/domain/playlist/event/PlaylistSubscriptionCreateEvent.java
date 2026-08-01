package com.codeit.mople.domain.playlist.event;

import java.util.UUID;

public record PlaylistSubscriptionCreateEvent(
    UUID ownerId,
    UUID playlistId,
    UUID subscriberId
) {

}
