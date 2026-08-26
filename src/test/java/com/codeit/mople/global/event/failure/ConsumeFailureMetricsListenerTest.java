package com.codeit.mople.global.event.failure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ListenerExecutionFailedException;

@DisplayName("ConsumeFailureMetricsListener 테스트")
class ConsumeFailureMetricsListenerTest {

  static final String COUNTER = "kafka.event.consume.failure";
  static final String TOPIC = "mople.follow.created.v1";

  SimpleMeterRegistry meterRegistry;
  ConsumeFailureMetricsListener listener;

  ConsumerRecord<String, Object> record;
  Exception exception;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    listener = new ConsumeFailureMetricsListener(meterRegistry);

    record = new ConsumerRecord<>(TOPIC, 2, 100L, "followee-key", new Object());
    exception = new IllegalStateException("알림 생성 실패");
  }

  double countOf(String reason) {
    return meterRegistry.get(COUNTER)
        .tag("topic", TOPIC)
        .tag("reason", reason)
        .counter()
        .count();
  }

  @Nested
  @DisplayName("소비 최종 실패 집계")
  class Recovered {

    @Test
    @DisplayName("DLT로 보내진 레코드를 토픽과 실패 원인별로 잘 세는지")
    void recoveredSuccess() {
      // when
      listener.recovered(record, exception);

      // then
      assertThat(countOf("IllegalStateException")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("DLT 발행까지 실패하면 그 실패 원인으로 세는지")
    void recoveryFailedSuccess() {
      // when
      listener.recoveryFailed(record, exception, new IllegalArgumentException("DLT 발행 실패"));

      // then
      assertThat(countOf("IllegalArgumentException")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("예외가 감싸여 오면 가장 안쪽 원인의 클래스 이름으로 세는지")
    void unwrapNestedCause() {
      // given
      Exception wrapped = new ListenerExecutionFailedException(
          "리스너 실행 실패", new IllegalArgumentException("잘못된 수신자"));

      // when
      listener.recovered(record, wrapped);

      // then
      assertThat(countOf("IllegalArgumentException")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("토픽과 원인이 같으면 하나의 시계열에 쌓이는지")
    void incrementSameSeries() {
      // when
      listener.recovered(record, exception);
      listener.recovered(record, new IllegalStateException("또 실패"));

      // then
      assertThat(countOf("IllegalStateException")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("아직 재시도 중인 실패는 세지 않는지")
    void failedDeliveryIsNotCounted() {
      // when
      listener.failedDelivery(record, exception, 1);

      // then
      Assertions.assertThatExceptionOfType(MeterNotFoundException.class)
          .isThrownBy(() -> meterRegistry.get(COUNTER).counter());
    }
  }
}