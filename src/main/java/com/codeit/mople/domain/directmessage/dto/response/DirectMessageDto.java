package com.codeit.mople.domain.directmessage.dto.response;

import com.codeit.mople.domain.directmessage.entity.DirectMessage;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.global.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {

  public static DirectMessageDto from(DirectMessage message) {
    User sender = message.getSender();
    User receiver = message.getReceiver();

    UserSummary senderSummary = new UserSummary(
        sender.getId(),
        sender.getName(),
        sender.getProfileImageUrl()
    );

    UserSummary receiverSummary = new UserSummary(
        receiver.getId(),
        receiver.getName(),
        receiver.getProfileImageUrl()
    );

    return new DirectMessageDto(
        message.getId(),
        message.getConversation().getId(),
        message.getCreatedAt(),
        senderSummary,
        receiverSummary,
        message.getContent()
    );
  }
}
