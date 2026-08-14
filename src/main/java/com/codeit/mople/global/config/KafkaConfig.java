package com.codeit.mople.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Configuration
public class KafkaConfig {

  public KafkaConfig(@Value("${spring.kafka.bootstrap-servers:}") String bootstrapServers) {
    Assert.state(StringUtils.hasText(bootstrapServers),
        "spring.kafka.bootstrap-servers 가 비어있습니다. KAFKA_BOOTSTRAP_SERVERS 를 설정하세요.");
  }
}
