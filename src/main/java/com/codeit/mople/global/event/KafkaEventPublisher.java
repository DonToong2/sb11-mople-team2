package com.codeit.mople.global.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(String topic, Object event) {
    kafkaTemplate.send(topic, event);
  }

}
