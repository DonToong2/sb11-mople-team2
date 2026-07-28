package com.codeit.mople.domain.follow.event;

import java.util.UUID;

public record FollowCreatedEvent(
    UUID followId,
    UUID followeeId,
    UUID followerId,
    String followerName
) {

}
