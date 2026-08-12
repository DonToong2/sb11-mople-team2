package com.codeit.mople.domain.follow.event;

import java.time.Instant;
import java.util.UUID;

public record FollowCreatedMessage(
    UUID eventId,
    Instant occurredAt,
    UUID followId,
    UUID followeeId,
    UUID followerId,
    String followerName
) {

  public static FollowCreatedMessage from(FollowCreatedEvent event) {
    return new FollowCreatedMessage(
        UUID.randomUUID(),
        Instant.now(),
        event.followId(),
        event.followeeId(),
        event.followerId(),
        event.followerName()
    );
  }
}