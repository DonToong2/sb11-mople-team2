package com.codeit.mople.domain.conversation.dto;

import com.codeit.mople.domain.conversation.entity.Conversation;
import com.codeit.mople.domain.directmessage.dto.DirectMessageDto;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.global.dto.UserSummary;
import java.util.UUID;

public record ConversationDto(
    UUID id,
    UserSummary with,
    DirectMessageDto lastestMessage,
    boolean hasUnread
) {

  public static ConversationDto from(Conversation conversation, UUID requesterId) {
    User withUser = conversation.getUserA().getId().equals(requesterId)
        ? conversation.getUserB()
        : conversation.getUserA();

    UserSummary withSummary = new UserSummary(
        withUser.getId(),
        withUser.getName(),
        withUser.getProfileImageUrl()
    );

    return new ConversationDto(
        conversation.getId(),
        withSummary,
        null,
        false
    );
  }
}
