package com.codeit.mople.domain.follow.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowRequest(
    @NotNull(message = "팔로우 대상 사용자 ID는 필수값입니다.") UUID followeeId
) {

}
