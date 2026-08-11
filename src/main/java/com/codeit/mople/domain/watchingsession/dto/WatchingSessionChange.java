package com.codeit.mople.domain.watchingsession.dto;

import com.codeit.mople.global.dto.UserSummary;
import com.fasterxml.jackson.annotation.JsonProperty;

public record WatchingSessionChange(
    String contentId,

    @JsonProperty("user")
    UserSummary user,

    //이벤트 타입(ENTER / LEAVE)
    @JsonProperty("type")
    String type,

    //실시간 시청자 수
    @JsonProperty("count")
    Long count
) {
}