package com.codeit.mople.domain.conversation.service;

import com.codeit.mople.domain.conversation.dto.response.ConversationDto;
import com.codeit.mople.domain.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.mople.domain.conversation.entity.Conversation;
import com.codeit.mople.domain.conversation.exception.ConversationErrorCode;
import com.codeit.mople.domain.conversation.repository.ConversationRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

  private final UserRepository userRepository;
  private final ConversationRepository conversationRepository;

  // TODO: 트랜잭션
  public ConversationDto findOrCreateConversation(UUID requesterId, UUID targetUserId) {
    log.debug("대화방 생성 시작 - requesterId: {}, targetUserId: {}", requesterId, targetUserId);

    if (requesterId.equals(targetUserId)) {
      throw new CustomException(ConversationErrorCode.INVALID_PARTICIPANT);
    }

    // userAId < userBId
    UUID userAId = (requesterId.compareTo(targetUserId) < 0) ? requesterId : targetUserId;
    UUID userBId = (userAId.equals(requesterId)) ? targetUserId : requesterId;

    User userA = userRepository.findById(userAId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    User userB = userRepository.findById(userBId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

    Conversation conversation;
    try {
      conversation = conversationRepository.findByUserAAndUserB(userA, userB)
          .orElseGet(() -> {
            log.info("기존 대화방 없음, 새 대화방 생성 - userAId: {}, userBId: {}", userAId, userBId);
            return conversationRepository.save(Conversation.createConversation(userA, userB));
          });
    } catch (DataIntegrityViolationException e) {
      log.info("동시 대화방 생성 충돌 발생, 기존 방 재조회 시도 - userAId: {}, userBId: {}", userAId, userBId);
      conversation = conversationRepository.findByUserAAndUserB(userA, userB)
          .orElseThrow(() -> new CustomException(ConversationErrorCode.CONVERSATION_NOT_FOUND));
    }

    log.info("대화방 생성 완료 - conversationId: {}", conversation.getId());
    return ConversationDto.from(conversation, requesterId);
  }

  public ConversationDto getConversation(UUID conversationId, UUID requesterId) {
    log.debug("대화방 단건 조회 요청 - conversationId: {}, requesterId: {}", conversationId, requesterId);

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new CustomException(ConversationErrorCode.CONVERSATION_NOT_FOUND));

    validateParticipant(conversation, requesterId);

    return ConversationDto.from(conversation, requesterId);
  }

  public ConversationDto getConversationWithUser(UUID requesterId, UUID targetUserId) {
    log.debug("상대 유저 지정을 통한 대화방 조회 요청 - requesterId: {}, targetUserId: {}", requesterId, targetUserId);

    UUID userAId = (requesterId.compareTo(targetUserId) < 0) ? requesterId : targetUserId;
    UUID userBId = (userAId.equals(requesterId)) ? targetUserId : requesterId;

    User userA = userRepository.findById(userAId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

    User userB = userRepository.findById(userBId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

    Conversation conversation = conversationRepository.findByUserAAndUserB(userA, userB)
        .orElseThrow(() -> new CustomException(ConversationErrorCode.CONVERSATION_NOT_FOUND));

    return ConversationDto.from(conversation, requesterId);
  }

  public CursorResponseConversationDto getMyConversations(UUID requesterId) {
    log.debug("내 대화방 목록 조회 요청 - requesterId: {}", requesterId);

    User requester = userRepository.findById(requesterId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

    List<Conversation> conversations = conversationRepository.findByUserAOrUserBOrderByCreatedAtDesc(requester);

    List<ConversationDto> conversationDtos = conversations.stream()
        .map(c -> ConversationDto.from(c, requesterId))
        .toList();

    return new CursorResponseConversationDto(
        conversationDtos,
        null,
        null,
        false,
        conversationDtos.size(),
        "createdAt",
        "DESCENDING"
    );
  }

  private void validateParticipant(Conversation conversation, UUID requesterId) {
    if (!conversation.getUserA().getId().equals(requesterId) && !conversation.getUserB().getId()
        .equals(requesterId)) {
      log.warn("대화방 인가 실패, 권한 없는 유저의 접근 - conversationId: {}, requesterId: {}", conversation.getId(), requesterId);
      throw new CustomException(ConversationErrorCode.ACCESS_DENIED);
    }
  }
}
