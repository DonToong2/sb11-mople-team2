package com.codeit.mople.domain.directmessage.service;

import com.codeit.mople.domain.conversation.entity.Conversation;
import com.codeit.mople.domain.conversation.exception.ConversationErrorCode;
import com.codeit.mople.domain.conversation.repository.ConversationRepository;
import com.codeit.mople.domain.directmessage.dto.response.DirectMessageDto;
import com.codeit.mople.domain.directmessage.entity.DirectMessage;
import com.codeit.mople.domain.directmessage.exception.DirectMessageErrorCode;
import com.codeit.mople.domain.directmessage.repository.DirectMessageRepository;
import com.codeit.mople.global.error.CustomException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageService {

  private final DirectMessageRepository directMessageRepository;
  private final ConversationRepository conversationRepository;

  // 특정 대화방의 메시지 목록 조회
  // TODO: 커서 페이지네이션 도입 예정
  public List<DirectMessageDto> getDirectMessages(UUID conversationId, UUID requesterId) {
    log.debug("특정 DM 목록 조회 요청 - conversationId: {}, requesterId: {}", conversationId, requesterId);
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new CustomException(ConversationErrorCode.CONVERSATION_NOT_FOUND));

    validateConversationParticipant(conversation, requesterId);

    List<DirectMessage> messages = directMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);

    log.info("특정 DM 목록 조회 완료 - conversationId: {}", conversationId);
    return messages.stream()
        .map(DirectMessageDto::from)
        .toList();
  }

  // 단건 메시지 읽음 처리
  @Transactional
  public void readMessage(UUID conversationId, UUID directMessageId, UUID requesterId) {
    log.debug("단건 DM 읽음 처리 요청 - messageId: {}, requesterId: {}", directMessageId, requesterId);

    DirectMessage message = directMessageRepository.findById(directMessageId)
        .orElseThrow(() -> new CustomException(DirectMessageErrorCode.DIRECT_MESSAGE_NOT_FOUND));

    if (!message.getConversation().getId().equals(conversationId)) {
      log.warn("대화방-DM 소속 불일치 - path conversationId: {}, actual conversationId: {}, messageId: {}",
          conversationId, message.getConversation().getId(), directMessageId);
      throw new CustomException(DirectMessageErrorCode.DIRECT_MESSAGE_NOT_FOUND);
    }

    if (!message.getReceiver().getId().equals(requesterId)) {
      log.warn("수신자가 아닌 유저의 접근, DM 읽음 처리 인가 실패 - messageId: {}, requesterId: {}", directMessageId, requesterId);
      throw new CustomException(DirectMessageErrorCode.UNAUTHORIZED_RECEIVER);
    }

    if (message.isRead()) {
      log.debug("이미 읽은 메시지이므로 추가 작업 생략 - messageId: {}", directMessageId);
      return;
    }

    message.markAsRead();
    log.info("DM 읽음 처리 완료 - messageId: {}", directMessageId);
  }

  // 공통 인가 로직 분리
  private void validateConversationParticipant(Conversation conversation, UUID requesterId) {
    if (!conversation.getUserA().getId().equals(requesterId) &&
    !conversation.getUserB().getId().equals(requesterId)) {
      throw new CustomException(ConversationErrorCode.ACCESS_DENIED);
    }
  }
}
