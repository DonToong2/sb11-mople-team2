package com.codeit.mople.global.config;

import com.codeit.mople.domain.directmessage.exception.DirectMessageException;
import com.codeit.mople.domain.notification.exception.NotificationException;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class KafkaConfig {

  private static final String DLT_SUFFIX = ".dlt";
  private static final int ANY_PARTITION = -1;

  static final BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> DLT_DESTINATION_RESOLVER =
      (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, ANY_PARTITION);

  public KafkaConfig(KafkaProperties kafkaProperties) {
    Assert.state(StringUtils.hasText(kafkaProperties.bootstrapServers()),
        "spring.kafka.enabled=true 이지만 spring.kafka.bootstrap-servers 가 비어있습니다. KAFKA_BOOTSTRAP_SERVERS 를 설정하세요.");
    log.info("Kafka 이벤트 발행 활성화: bootstrapServers={}", kafkaProperties.bootstrapServers());
  }

  @Bean
  public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    // Consumer가 실패했을 때 그 메세지를 (원래토픽명 + .dlt)로 재발행
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(kafkaTemplate, DLT_DESTINATION_RESOLVER);

    // Exponential : 지수
    // 지수백오프 구현을 위한 객체 생성
    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);

    backOff.setInitialInterval(1000L); // 초기 재시도 대기 시간(1초)
    backOff.setMultiplier(2.0); // 다음 재시도에 곱해지는 시간(2배)
    backOff.setMaxInterval(4000L); // 최대 재시도 대기 시간(4초)

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // SSE 관련 도메인 객체(DM, 알림) 예외 발생 시 재시도 하지 않음(not found 에러 등)
    errorHandler.addNotRetryableExceptions(
        DirectMessageException.class,
        NotificationException.class
    );

    return errorHandler;
  }

}