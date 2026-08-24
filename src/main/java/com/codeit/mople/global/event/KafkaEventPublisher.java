package com.codeit.mople.global.event;

import com.codeit.mople.global.config.KafkaProperties;
import com.codeit.mople.global.event.failure.FailedEvent;
import com.codeit.mople.global.event.failure.FailedEventStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
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
    try {
      kafkaTemplate.send(topic, key, event)
          .whenComplete((result, ex) -> {
            if (ex != null) {
              handleFailure(topic, key, event, ex, "Kafka 이벤트 발행 최종 실패");
            }
          });
    } catch (SerializationException e) {
      handleFailure(topic, key, event, e, "Kafka 이벤트 직렬화 실패");
    } catch (Exception e) {
      handleFailure(topic, key, event, e, "Kafka 이벤트 발행 시도 실패");
    }
  }

  private void handleFailure(
      String topic,
      String key,
      PublishableEvent event,
      Throwable cause,
      String message
  ) {
    failedEventStore.save(FailedEvent.of(topic, key, event, serialize(topic, key, event), cause));

    log.error("{}: topic={}, key={}, eventId={}, eventType={}",
        message, topic, key, event.eventId(), event.getClass().getSimpleName(), cause);
  }

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
