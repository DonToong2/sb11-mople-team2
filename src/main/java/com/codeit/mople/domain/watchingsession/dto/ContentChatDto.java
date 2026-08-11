package com.codeit.mople.domain.watchingsession.dto;

import com.codeit.mople.global.dto.UserSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record ContentChatDto(

    String contentId,

    UUID senderId,

    String senderName,

    @JsonProperty("sender")
    UserSummary sender,

    @JsonProperty("user")
    UserSummary user,

    String message,

    @JsonProperty("content")
    String content,

    Instant timestamp,

    @JsonProperty("createdAt")
    Instant createdAt
) {

}