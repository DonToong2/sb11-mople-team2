package com.codeit.mople.global.event;

import com.codeit.mople.global.config.KafkaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class KafkaEventPublisher {

  private static final String FAILED_STREAM_KEY = "kafka:events:failed";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;


  public void publish(String topic, PublishableEvent event) {
    UUID eventId = event.eventId();
    String eventType = event.getClass().getSimpleName();
    try {
      kafkaTemplate.send(topic, event)
          .whenComplete((result, ex) -> {
            if (ex != null) {
              saveFailedEvent(topic, null, event, ex);
              log.error("Kafka 이벤트 발행 최종 실패: topic={}, eventId={}, eventType={}",
                  topic, eventId, eventType, ex);
            }
          });
    } catch (SerializationException e) {
      log.error("Kafka 이벤트 직렬화 실패(재시도 불가): topic={}, eventId={}, eventType={}",
          topic, eventId, eventType, e);
    } catch (Exception e) {
      log.error("Kafka 이벤트 발행 시도 실패: topic={}, eventId={}, eventType={}",
          topic, eventId, eventType, e);
    }
  }

  public void publish(String topic, String key, PublishableEvent event) {
    UUID eventId = event.eventId();
    String eventType = event.getClass().getSimpleName();
    try {
      kafkaTemplate.send(topic, key, event)
          .whenComplete((result, ex) -> {
            if (ex != null) {
              saveFailedEvent(topic, key, event, ex);
              log.error("Kafka 이벤트 발행 최종 실패: topic={}, key={}, eventId={}, eventType={}",
                  topic, key, eventId, eventType, ex);
            }
          });
    } catch (SerializationException e) {
      log.error("Kafka 이벤트 직렬화 실패(재시도 불가): topic={}, key={}, eventId={}, eventType={}",
          topic, key, eventId, eventType, e);
    } catch (Exception e) {
      log.error("Kafka 이벤트 발행 시도 실패: topic={}, key={}, eventId={}, eventType={}",
          topic, key, eventId, eventType, e);
    }
  }

  private void saveFailedEvent(String topic, String key, Object event, Throwable ex) {
    try {
      String data = objectMapper.writeValueAsString(event);

      redisTemplate.opsForStream().add(
          FAILED_STREAM_KEY,
          Map.of(
              "type", "PRODUCER",
              "topic", topic,
              "key", key == null ? "" : key,
              "data", data,
              "error", ex == null ? "Unknown" : String.valueOf(ex.getMessage())
          )
      );
    } catch (JsonProcessingException e) {
      log.error("Kafka Producer 최종 실패 이벤트 직렬화 실패: topic={}, key={}"
          , topic, key, e);
    } catch (Exception e) {
      log.error("Kafka Producer 최종 실패 이벤트 Redis 저장 실패: topic={}, key={}",
          topic, key, e);
    }
  }

}
