package com.codeit.mople.domain.playlist.event;

import com.codeit.mople.global.event.KafkaEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class PlaylistContentAddedEventRelay {

  private final KafkaEventPublisher publisher;
  private final String topic;

  public PlaylistContentAddedEventRelay(KafkaEventPublisher publisher,
      @Value("${kafka.topics.playlist-content-added}") String topic) {
    this.publisher = publisher;
    this.topic = topic;
  }

  @Async("kafkaRelayExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void relay(PlaylistContentAddedEvent event) {
    PlaylistContentAddedMessage message = PlaylistContentAddedMessage.from(event);
    String key = message.subscriberId().toString();

    log.debug("플레이리스트 콘텐츠 추가 이벤트 발행: playlistContentId={}, subscriberId={}, eventId={}",
        message.playlistContentId(), message.subscriberId(), message.eventId());
    publisher.publish(topic, key, message);
  }
}
