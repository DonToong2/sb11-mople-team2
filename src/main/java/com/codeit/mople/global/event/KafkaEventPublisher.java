package com.codeit.mople.global.event;

import com.codeit.mople.global.config.KafkaProperties;
import com.codeit.mople.global.event.failure.FailedEvent;
import com.codeit.mople.global.event.failure.FailedEventStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class KafkaEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final FailedEventStore failedEventStore;
  private final ObjectMapper objectMapper;


  public void publish(String topic, PublishableEvent event) {
    publish(topic, null, event);
  }

  public void publish(String topic, String key, PublishableEvent event) {
    send(topic, key, event).whenComplete((result, cause) -> {
      if (cause != null) {
        // 실패를 하나의 객체로 생성
        handleFailure(topic, key, event, cause);
      }
    });
  }

  // 브로커에 발행 시도
  // CompletableFuture: 이것을 사용하여 미리 결과를 기다리지 않고 즉시 리턴, 리턴값은 비동기로 채워짐
  private CompletableFuture<SendResult<String, Object>> send(
      String topic,
      String key,
      PublishableEvent event
  ) {
    try {
      return kafkaTemplate.send(topic, key, event);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  // 프로듀서 최종 실패를 하나의 객채로 생성 및 로그 기록(여기서 만들어진 객체는 redis에 저장할 객체임)
  private void handleFailure(String topic, String key, PublishableEvent event, Throwable cause) {
    failedEventStore.save(FailedEvent.of(topic, key, event, serialize(topic, key, event), cause));

    log.error("{}: topic={}, key={}, eventId={}, eventType={}",
        reasonOf(cause), topic, key, event.eventId(), event.getClass().getSimpleName(), cause);
  }

  // cause가 serializationException타입이면 "Kafka 이벤트 직렬화 실패" 아니면 "Kafka 이벤트 발행 최종 실패"
  private String reasonOf(Throwable cause) {
    return cause instanceof SerializationException
        ? "Kafka 이벤트 직렬화 실패"
        : "Kafka 이벤트 발행 최종 실패";
  }

  // event를 JSON 문자열로 직렬화 Redis Stream은 문자열 값만 저장 가능해서 변환 필요
  // 변환 도중 문제가 생기면 로그 기록
  private String serialize(String topic, String key, PublishableEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error("Kafka Producer 실패 이벤트 본문 직렬화 실패: topic={}, key={}, eventId={}, eventType={}",
          topic, key, event.eventId(), event.getClass().getSimpleName(), e);
      return "";
    }
  }
}