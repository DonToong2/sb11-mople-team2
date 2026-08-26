package com.codeit.mople.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaConfig 테스트")
class KafkaConfigTest {

  static final String TOPIC = "mople.follow.created.v1";

  ConsumerRecord<String, Object> record;
  RuntimeException exception;

  @BeforeEach
  void setUp() {
    record = new ConsumerRecord<>(TOPIC, 2, 100L, "followee-key", new Object());
    exception = new RuntimeException("알림 생성 실패");
  }

  @Nested
  @DisplayName("DLT 목적지 해석")
  class DeadLetterDestination {

    @Test
    @DisplayName("원 토픽 이름 뒤에 .dlt를 붙인 토픽으로 보내는지")
    void resolveDltTopic() {
      // when
      TopicPartition destination =
          KafkaConfig.DLT_DESTINATION_RESOLVER.apply(record, exception);

      // then
      assertThat(destination.topic()).isEqualTo("mople.follow.created.v1.dlt");
    }

    @Test
    @DisplayName("파티션을 지정하지 않아서 DLT 파티션 수가 더 적어도 발행이 안 깨지는지")
    void resolveAnyPartition() {
      // when
      TopicPartition destination =
          KafkaConfig.DLT_DESTINATION_RESOLVER.apply(record, exception);

      // then
      assertThat(destination.partition()).isEqualTo(-1);
    }
  }

  @Nested
  @DisplayName("DLT 발행 템플릿 선택")
  class DltTemplates {

    @Mock
    KafkaTemplate<String, Object> jsonTemplate;
    @Mock
    KafkaTemplate<String, byte[]> bytesTemplate;

    @Test
    @DisplayName("byte[] 가 Object 보다 먼저 와야 역직렬화 실패분이 바이트 템플릿으로 가는지")
    void bytesTemplateComesFirst() {
      // when
      Map<Class<?>, KafkaOperations<?, ?>> templates =
          KafkaConfig.dltTemplates(jsonTemplate, bytesTemplate);

      // then
      assertThat(templates.keySet()).containsExactly(byte[].class, Object.class);
      assertThat(templates.get(byte[].class)).isSameAs(bytesTemplate);
      assertThat(templates.get(Object.class)).isSameAs(jsonTemplate);
    }
  }
}