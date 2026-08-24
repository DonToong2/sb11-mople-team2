package com.codeit.mople.domain.playlist.event;

import com.codeit.mople.domain.playlist.repository.search.PlaylistDocument;
import com.codeit.mople.domain.playlist.repository.search.PlaylistSearchRepository;
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
@KafkaListener(topics = "playlist-search-index-events")
public class PlaylistSearchIndexConsumer {

  private final PlaylistSearchRepository playlistSearchRepository;
  private final ProcessedEventRepository processedEventRepository;

  @KafkaHandler
  public void handle(PlaylistSearchIndexEvent event) {
    log.debug("플레이리스트 검색 인덱스 반영 시도: playlistId={}",
        event.playlistId());

    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    playlistSearchRepository.save(
        new PlaylistDocument(
            event.playlistId(),
            event.title(),
            event.subscribeCount(),
            event.updatedAt()
        )
    );

    markProcessed(event.eventId());

    log.info("플레이리스트 검색 인덱스 반영 완료: playlistId={}",
        event.playlistId());
  }

  @KafkaHandler
  public void handle(PlaylistSearchIndexDeleteEvent event) {
    log.debug("플레이리스트 검색 인덱스 삭제 시도: playlistId={}",
        event.playlistId());

    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    playlistSearchRepository.deleteById(event.playlistId());

    markProcessed(event.eventId());

    log.info("플레이리스트 검색 인덱스 삭제 완료: playlistId={}",
        event.playlistId());
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