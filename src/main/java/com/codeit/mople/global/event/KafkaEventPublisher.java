package com.codeit.mople.global.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(String topic, Object event) {
    kafkaTemplate.send(topic, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Kafka 이벤트 발행 최종 실패: topic={}", topic, ex);
          }
        });
  }

  public void publish(String topic, String key, Object event) {
    kafkaTemplate.send(topic, key, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Kafka 이벤트 발행 최종 실패: topic={}", topic, ex);
          }
        });
  }

}
