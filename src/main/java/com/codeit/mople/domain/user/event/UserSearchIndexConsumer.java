package com.codeit.mople.domain.user.event;

import com.codeit.mople.domain.user.repository.search.UserDocument;
import com.codeit.mople.domain.user.repository.search.UserSearchRepository;
import com.codeit.mople.global.config.KafkaProperties;
import com.codeit.mople.global.event.processed.ProcessedEvent;
import com.codeit.mople.global.event.processed.ProcessedEventRepository;
import com.codeit.mople.global.event.processed.ProcessedEventStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = KafkaProperties.PREFIX,
    name = "enabled",
    havingValue = "true"
)
@KafkaListener(topics = "user-search-index-events")
public class UserSearchIndexConsumer {

  private final UserSearchRepository userSearchRepository;
  private final ProcessedEventRepository processedEventRepository;

  @KafkaHandler
  public void handle(UserSearchIndexEvent event) {
    log.debug("사용자 검색 인덱스 반영 시도: userId={}",
        event.userId());

    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    userSearchRepository.save(
        new UserDocument(
            event.userId(),
            event.email(),
            event.name(),
            event.createdAt(),
            event.locked(),
            event.role()
        )
    );

    markProcessed(event.eventId());

    log.info("사용자 검색 인덱스 반영 완료: userId={}",
        event.userId());
  }

  private boolean checkAndRecordProcessedEvent(UUID eventId) {
    int inserted = processedEventRepository.insertIfAbsent(eventId);

    if (inserted == 0) {
      ProcessedEvent processedEvent = processedEventRepository.findById(eventId).orElseThrow(() ->
                  new IllegalStateException("처리 이벤트를 찾을 수 없습니다: eventId=" + eventId));

      if (processedEvent.getStatus() == ProcessedEventStatus.PROCESSED) {
        log.info("이미 처리된 이벤트입니다: eventId={}", eventId);
        return true;
      }

      log.info("처리되지 않은 이벤트를 재처리합니다: eventId={}", eventId);
    }

    return false;
  }

  private void markProcessed(UUID eventId) {
    ProcessedEvent processedEvent = processedEventRepository.findById(eventId).orElseThrow(() ->
                new IllegalStateException("처리 이벤트를 찾을 수 없습니다: eventId=" + eventId));

    // PENDING → PROCESSED
    processedEvent.markProcessed();

    processedEventRepository.save(processedEvent);
  }
}