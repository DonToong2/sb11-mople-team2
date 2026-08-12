package com.codeit.mople.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaConfig {

  // 토픽을 나눠 담는 단위(동시에 몇 개 처리할지?)
  private static final int PARTITIONS = 1;
  // 파티션 단위의 복제본 개수
  private static final short REPLICAS = 1;

  // 앱이 실행되면 브로커에 kafka.topics.follow-created에 있는 이름으로 topic을 만들어라
  @Bean
  public NewTopic followCreatedTopic(@Value("${kafka.topics.follow-created}")String topic) {
    return TopicBuilder.name(topic)
        .partitions(PARTITIONS)
        .replicas(REPLICAS)
        .build();
  }
}
