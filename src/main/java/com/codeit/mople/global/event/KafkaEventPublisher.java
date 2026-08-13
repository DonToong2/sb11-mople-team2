package com.codeit.mople.global.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

  private static final String FAILED_STREAM_KEY = "kafka:events:failed";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public void publish(String topic, Object event) {
    kafkaTemplate.send(topic, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            saveFailedEvent(topic, null, event, ex);
            log.error("Kafka 이벤트 발행 최종 실패: topic={}", topic, ex);
          }
        });
  }

  public void publish(String topic, String key, Object event) {
    kafkaTemplate.send(topic, key, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            saveFailedEvent(topic, key, event, ex);
            log.error("Kafka 이벤트 발행 최종 실패: topic={}", topic, ex);
          }
        });
  }

  private void saveFailedEvent(String topic, String key, Object event, Throwable ex) {
    try {
      String data = objectMapper.writeValueAsString(event);

      redisTemplate.opsForStream().add(
          FAILED_STREAM_KEY,
          Map.of(
              "topic", topic,
              "key", key == null ? "" : key,
              "data", data,
              "error", ex == null ? "Unknown" : String.valueOf(ex.getMessage())
          )
      );
    } catch (JsonProcessingException e) {
      log.error("Kafka 최종 실패 이벤트 직렬화 실패: topic={}, key={}", topic, key, e);
    }
  }

}
