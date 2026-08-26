package com.codeit.mople.global.event.failure;

import com.codeit.mople.global.config.KafkaProperties;
import com.codeit.mople.global.event.PublishableEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class FailedEventReplayer {

  private static final String TRUSTED_PACKAGE = "com.codeit.mople.";
  private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(60);

  private final FailedEventStore failedEventStore;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public FailedEventReplayResult replay(FailedEventQuery query) {
    List<FailedEvent> targets = failedEventStore.find(query);
    List<FailedEvent> replayable = targets.stream().filter(FailedEvent::replayable).toList();

    int replayed = confirmAndDelete(dispatchAll(replayable));
    int skipped = targets.size() - replayable.size();
    int failed = targets.size() - skipped - replayed;

    log.info("실패 이벤트 재발행 완료: 대상={}, 재발행={}, 본문없음={}, 실패={}",
        targets.size(), replayed, skipped, failed);

    return new FailedEventReplayResult(targets.size(), replayed, skipped, failed);
  }

  // Redis에 쌓인 발행 실패 이벤트들을 역직렬화 하여 브로커로 재발항 시도하는 메서드
  private List<Dispatch> dispatchAll(List<FailedEvent> targets) {
    List<Dispatch> dispatches = new ArrayList<>();

    for (FailedEvent target : targets) {
      try {
        PublishableEvent event = restore(target);

        // 발행 최종실패한 이벤트를 다시 발행하면서 topic을 남기도록함 왜냐하면 이것을 알아야 redis에 저장되어있는 원본 이벤트를 찾아서 삭제가능
        dispatches.add(new Dispatch(
            target, kafkaTemplate.send(target.topic(), target.keyOrNull(), event)));
      } catch (Exception e) {
        log.error("실패 이벤트 재발행 시도 실패: recordId={}, topic={}, eventType={}",
            target.recordId(), target.topic(), target.eventType(), e);
      }
    }

    return dispatches;
  }

  // 재발행 시도 성공한것만 골라서 Redis에서 원본을 지우는 메서드
  private int confirmAndDelete(List<Dispatch> dispatches) {
    awaitAll(dispatches);

    int replayed = 0;

    for (Dispatch dispatch : dispatches) {
      if (delivered(dispatch)) {
        deleteOriginal(dispatch.target());
        replayed++;
      }
    }

    return replayed;
  }

  private void deleteOriginal(FailedEvent target) {
    try {
      failedEventStore.delete(target.recordId());
    } catch (Exception e) {
      log.error("재발행한 실패 이벤트의 원본 삭제 실패: recordId={}, topic={}, eventType={}",
          target.recordId(), target.topic(), target.eventType(), e);
    }
  }

  // 프로듀서 최종 실패 리스트를 받아서
  private void awaitAll(List<Dispatch> dispatches) {
    // 프로듀서 최종 실패 각각의 sending()을 호출해서 그 안에 있는 CompletableFuture 값을 꺼내오겠다
    CompletableFuture<?>[] sendings = dispatches.stream()
        .map(Dispatch::sending)
        .toArray(CompletableFuture[]::new);

    try {
      CompletableFuture.allOf(sendings).get(CONFIRM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.warn("실패 이벤트 재발행 결과 대기 종료: 대상={}", dispatches.size(), e);
    }
  }

  private boolean delivered(Dispatch dispatch) {
    FailedEvent target = dispatch.target();

    if (!dispatch.sending().isDone()) {
      log.error("실패 이벤트 재발행 결과 미확인: recordId={}, topic={}, eventType={}",
          target.recordId(), target.topic(), target.eventType());

      return false;
    }

    try {
      dispatch.sending().join();

      return true;
    } catch (Exception e) {
      log.error("실패 이벤트 재발행 실패: recordId={}, topic={}, eventType={}",
          target.recordId(), target.topic(), target.eventType(), e);

      return false;
    }
  }

  private PublishableEvent restore(FailedEvent target)
      throws ClassNotFoundException, JsonProcessingException {
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

  private record Dispatch(
      FailedEvent target,
      CompletableFuture<SendResult<String, Object>> sending
  ) {

  }
}