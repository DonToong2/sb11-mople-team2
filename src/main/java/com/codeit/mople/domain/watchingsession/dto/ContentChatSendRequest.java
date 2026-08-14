package com.codeit.mople.domain.watchingsession.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ContentChatSendRequest(
    @JsonProperty("message")
    @JsonAlias({"content", "text"})
    String message
) {

}
