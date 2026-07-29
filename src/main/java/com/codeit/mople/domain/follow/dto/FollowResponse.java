package com.codeit.mople.domain.follow.dto;

import java.util.UUID;

public record FollowResponse(
    UUID id,
    UUID followeeId,
    UUID followerId
) {

}
