package com.codeit.mople.domain.conversation.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto(
    List<ConversationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {

}
