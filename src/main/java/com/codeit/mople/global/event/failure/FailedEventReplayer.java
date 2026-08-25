package com.codeit.mople.global.event.failure;

import com.codeit.mople.global.config.KafkaProperties;
import com.codeit.mople.global.event.KafkaEventPublisher;
import com.codeit.mople.global.event.PublishableEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class FailedEventReplayer {
  
  private static final String TRUSTED_PACKAGE = "com.codeit.mople.";

  private final FailedEventStore failedEventStore;
  private final KafkaEventPublisher publisher;
  private final ObjectMapper objectMapper;

  public FailedEventReplayResult replay(Duration within, int limit) {
    List<FailedEvent> targets = failedEventStore.findRecent(within, limit);

    int replayed = 0;
    int skipped = 0;
    int failed = 0;

    for (FailedEvent target : targets) {
      if (!target.replayable()) {
        skipped++;
        continue;
      }

      if (republish(target)) {
        replayed++;
      } else {
        failed++;
      }
    }

    log.info("실패 이벤트 재발행 완료: 대상={}, 재발행={}, 본문없음={}, 실패={}",
        targets.size(), replayed, skipped, failed);

    return new FailedEventReplayResult(targets.size(), replayed, skipped, failed);
  }
  
  private boolean republish(FailedEvent target) {
    try {
      publisher.publish(target.topic(), target.keyOrNull(), restore(target));
      failedEventStore.delete(target.recordId());

      return true;
    } catch (Exception e) {
      log.error("실패 이벤트 재발행 실패: recordId={}, topic={}, eventType={}",
          target.recordId(), target.topic(), target.eventType(), e);

      return false;
    }
  }

  private PublishableEvent restore(FailedEvent target) throws Exception {
    String eventType = target.eventType();

    if (!eventType.startsWith(TRUSTED_PACKAGE)) {
      throw new IllegalArgumentException("허용되지 않은 이벤트 타입입니다: " + eventType);
    }

    Class<?> type = Class.forName(eventType);

    if (!PublishableEvent.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException("발행 가능한 이벤트가 아닙니다: " + eventType);
    }

    return (PublishableEvent) objectMapper.readValue(target.data(), type);
  }
}