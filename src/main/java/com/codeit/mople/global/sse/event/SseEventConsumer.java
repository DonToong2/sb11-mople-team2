package com.codeit.mople.global.sse.event;

import com.codeit.mople.domain.directmessage.dto.response.DirectMessageDto;
import com.codeit.mople.domain.directmessage.entity.DirectMessage;
import com.codeit.mople.domain.directmessage.event.DirectMessageCreatedEvent;
import com.codeit.mople.domain.directmessage.exception.DirectMessageErrorCode;
import com.codeit.mople.domain.directmessage.exception.DirectMessageException;
import com.codeit.mople.domain.directmessage.repository.DirectMessageRepository;
import com.codeit.mople.domain.notification.dto.response.NotificationResponse;
import com.codeit.mople.domain.notification.entity.Notification;
import com.codeit.mople.domain.notification.event.NotificationCreatedEvent;
import com.codeit.mople.domain.notification.exception.NotificationErrorCode;
import com.codeit.mople.domain.notification.exception.NotificationException;
import com.codeit.mople.domain.notification.repository.NotificationRepository;
import com.codeit.mople.global.event.processed.ProcessedEventRepository;
import com.codeit.mople.global.sse.service.SseService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventConsumer {

  private final SseService sseService;
  private final DirectMessageRepository directMessageRepository;
  private final NotificationRepository notificationRepository;
  private final ProcessedEventRepository processedEventRepository;

  @KafkaListener(topics = "direct-message-created")
  @Transactional
  public void handle(DirectMessageCreatedEvent event) {

    log.debug("SSE 이벤트 전송 시도: receiverId={}, directMessageId={}",
        event.receiverId(), event.directMessageId());

    DirectMessage directMessage = directMessageRepository.findById(event.directMessageId())
        .orElseThrow(() ->
            new DirectMessageException(
                DirectMessageErrorCode.DIRECT_MESSAGE_NOT_FOUND,
                Map.of("directMessageId", event.directMessageId())
            )
        );

    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    DirectMessageDto directMessageDto = DirectMessageDto.from(directMessage);

    // SSE 이벤트 전송
    sseService.send(
        event.receiverId(),
        "direct-messages",
        directMessageDto
    );

    log.info("SSE 전송 완료: receiverId={}, directMessageId={}",
        event.receiverId(), event.directMessageId());
  }

  @KafkaListener(topics = "notification-created")
  @Transactional
  public void handle(NotificationCreatedEvent event) {

    log.debug("SSE 이벤트 전송 시도: receiverId={}, notificationId={}",
        event.receiverId(), event.notificationId());

    Notification notification = notificationRepository.findById(event.notificationId())
        .orElseThrow(() ->
            new NotificationException(
                NotificationErrorCode.NOTIFICATION_NOT_FOUND,
                Map.of("notificationId", event.notificationId()))
        );

    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    NotificationResponse response = NotificationResponse.from(notification);

    sseService.send(
        event.receiverId(),
        "notifications",
        response
    );

    log.info("알림 SSE 전송 완료: receiverId={}, notificationId={}",
        event.receiverId(), event.notificationId());
  }

  private boolean checkAndRecordProcessedEvent(UUID eventId) {
    // 이미 해당 eventId가 존재하면 스킵
    int inserted = processedEventRepository.insertIfAbsent(eventId);

    if (inserted == 0) {
      log.info("이미 처리된 이벤트입니다: eventId={}", eventId);
      return true;
    }

    return false;
  }

}
