package com.codeit.mople.global.config;

import com.codeit.mople.domain.directmessage.exception.DirectMessageException;
import com.codeit.mople.domain.notification.exception.NotificationException;
import com.codeit.mople.global.event.failure.ConsumeFailureMetricsListener;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
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
  public DefaultErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, Object> kafkaTemplate,
      ConsumeFailureMetricsListener consumeFailureMetricsListener
  ) {
    // DLT로 메세지 보내주는 객체: Consumer가 실패했을 때 그 메세지를 (원래토픽명 + .dlt)로 재발행
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(kafkaTemplate, DLT_DESTINATION_RESOLVER);

    // Exponential : 지수
    // 지수백오프 구현을 위한 객체 생성
    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);

    backOff.setInitialInterval(1000L); // 초기 재시도 대기 시간(1초)
    backOff.setMultiplier(2.0); // 다음 재시도에 곱해지는 시간(2배)
    backOff.setMaxInterval(4000L); // 최대 재시도 대기 시간(4초)

    // 재시도 recoverer 이것을 재시도
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // SSE 관련 도메인 객체(DM, 알림) 예외 발생 시 재시도 하지 않음(not found 에러 등)
    errorHandler.addNotRetryableExceptions(
        DirectMessageException.class,
        NotificationException.class
    );

    errorHandler.setRetryListeners(consumeFailureMetricsListener);

    return errorHandler;
  }

  // 프로듀서 메세지 직렬화 하는 방식
  @Bean
  public DefaultKafkaProducerFactoryCustomizer kafkaValueSerializerCustomizer() {
    return KafkaConfig::applyValueSerializer;
  }

  // 프로듀서가 브로커로 메세지를 전송하기 전에 직렬화방식 설정
  // 값이 byte[]면 바이트 그대로 전송, 그 외 일반 객체면 JSON으로 변환해서 전송
  @SuppressWarnings("unchecked")
  private static void applyValueSerializer(DefaultKafkaProducerFactory<?, ?> producerFactory) {
    ((DefaultKafkaProducerFactory<Object, Object>) producerFactory).setValueSerializer(
        new DelegatingByTypeSerializer(
            Map.of(
                byte[].class, new ByteArraySerializer(),
                Object.class, new JsonSerializer<>()
            ),
            true
        )
    );
  }

}