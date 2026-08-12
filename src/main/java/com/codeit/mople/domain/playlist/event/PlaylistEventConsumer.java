package com.codeit.mople.domain.playlist.event;

import com.codeit.mople.domain.playlist.repository.PlaylistRepository;
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

  @KafkaListener(topics = "playlist-subscribed")
  @Transactional
  public void handle(PlaylistSubscribedEvent event) {

    playlistRepository.increaseSubscriberCount(event.playlistId());

    log.info("플레이리스트 구독자 수 증가 완료: playlistId={}",
        event.playlistId());
  }

  @KafkaListener(topics = "playlist-unsubscribed")
  @Transactional
  public void handle(PlaylistUnsubscribedEvent event) {

    int decreased = playlistRepository.decreaseSubscriberCount(event.playlistId());
    if (decreased == 0) {
      log.warn("구독자 수 감소 실패(이미 0): playlistId={}, subscriberId={}",
          event.playlistId(), event.subscriberId());

      return;
    }
    log.info("플레이리스트 구독자 수 감소 완료: playlistId={}",
        event.playlistId());
  }

}
