package com.codeit.mople.domain.watchingsession.dto;

import java.util.UUID;

public record ChatMessageRequest(
    UUID senderId,
    String senderName,
    String message
) {

}
