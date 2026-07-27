package com.codeit.mople.domain.conversation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
    @NotNull(message = "대화할 상대방의 유저 ID는 필수값입니다.")
    UUID withUserId
) {

}
