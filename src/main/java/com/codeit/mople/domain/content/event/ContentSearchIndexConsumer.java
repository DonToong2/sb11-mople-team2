package com.codeit.mople.domain.content.event;

import com.codeit.mople.domain.content.repository.search.ContentDocument;
import com.codeit.mople.domain.content.repository.search.ContentSearchRepository;
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

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = KafkaProperties.PREFIX,
    name = "enabled",
    havingValue = "true"
)
@KafkaListener(topics = "content-search-index-events")
public class ContentSearchIndexConsumer {

  private final ContentSearchRepository contentSearchRepository;
  private final ProcessedEventRepository processedEventRepository;

  @KafkaHandler
  public void handle(ContentSearchIndexEvent event) {
    log.debug("콘텐츠 검색 인덱스 반영 시도: contentId={}",
        event.contentId());

    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    contentSearchRepository.save(
        new ContentDocument(
            event.contentId(),
            event.title(),
            event.type(),
            event.rating(),
            event.watcherCount(),
            event.createdAt()
        )
    );

    markProcessed(event.eventId());

    log.info("콘텐츠 검색 인덱스 반영 완료: contentId={}",
        event.contentId());
  }

  @KafkaHandler
  public void handle(ContentSearchIndexDeleteEvent event) {
    log.debug("콘텐츠 검색 인덱스 삭제 시도: contentId={}",
        event.contentId());

    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    contentSearchRepository.deleteById(event.contentId());

    markProcessed(event.eventId());

    log.info("콘텐츠 검색 인덱스 삭제 완료: contentId={}",
        event.contentId());
  }

  private boolean checkAndRecordProcessedEvent(UUID eventId) {
    // (eventId, PENDING)을 삽입하되 이미 eventId가 DB에 존재하면 0을 반환
    int inserted = processedEventRepository.insertIfAbsent(eventId);

    // 이미 eventId가 존재하면
    if (inserted == 0) {
      ProcessedEvent processedEvent = processedEventRepository.findById(eventId).orElseThrow(() ->
                  new IllegalStateException("처리 이벤트를 찾을 수 없습니다: eventId=" + eventId));

      // (eventId, PROCESSED)가 있을 경우 스킵
      if (processedEvent.getStatus() == ProcessedEventStatus.PROCESSED) {
        log.info("이미 처리된 이벤트입니다: eventId={}", eventId);
        return true;
      }

      log.info("처리되지 않은 이벤트를 재처리합니다: eventId={}", eventId);
    }

    // 기존 DB에 존재하지 않은 신규 (eventId, PENDING)인 상태,(스킵X)
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