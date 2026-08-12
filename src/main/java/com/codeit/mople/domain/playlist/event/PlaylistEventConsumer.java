package com.codeit.mople.domain.playlist.event;

import com.codeit.mople.domain.playlist.repository.PlaylistRepository;
import com.codeit.mople.global.event.processed.ProcessedEvent;
import com.codeit.mople.global.event.processed.ProcessedEventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistEventConsumer {

  private final PlaylistRepository playlistRepository;

  private final ProcessedEventRepository processedEventRepository;

  @KafkaListener(topics = "playlist-subscribed")
  @Transactional
  public void handle(PlaylistSubscribedEvent event) {
    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    playlistRepository.increaseSubscriberCount(event.playlistId());

    log.info("플레이리스트 구독자 수 증가 완료: playlistId={}",
        event.playlistId());
  }

  @KafkaListener(topics = "playlist-unsubscribed")
  @Transactional
  public void handle(PlaylistUnsubscribedEvent event) {
    if (checkAndRecordProcessedEvent(event.eventId())) {
      return;
    }

    int decreased = playlistRepository.decreaseSubscriberCount(event.playlistId());
    if (decreased == 0) {
      log.warn("구독자 수 감소 실패(이미 0): playlistId={}, subscriberId={}",
          event.playlistId(), event.subscriberId());

      return;
    }
    log.info("플레이리스트 구독자 수 감소 완료: playlistId={}",
        event.playlistId());
  }

  private boolean checkAndRecordProcessedEvent(UUID eventId) {
    // 이미 해당 eventId가 존재하면 스킵
    if (processedEventRepository.existsByEventId(eventId)) {
      log.info("이미 처리된 이벤트입니다: eventId={}", eventId);
      return true;
    }

    processedEventRepository.save(ProcessedEvent.of(eventId));
    return false;
  }

}
