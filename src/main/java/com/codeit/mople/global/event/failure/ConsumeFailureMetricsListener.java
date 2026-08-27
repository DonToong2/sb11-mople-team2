package com.codeit.mople.global.event.failure;

import com.codeit.mople.global.config.KafkaProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class ConsumeFailureMetricsListener implements RetryListener {

  private static final String FAILURE_COUNTER = "kafka.event.consume.failure";
  private static final String DESCRIPTION = "소비 최종 실패로 DLT 에 보내진 레코드 수";

  private static final String TAG_TOPIC = "topic";
  private static final String TAG_REASON = "reason";
  private static final String UNKNOWN_REASON = "Unknown";

  private final MeterRegistry meterRegistry;

  // 아무런 기능 없음
  @Override
  public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
  }

  // 재시도 소진 후 DLT 발행 성공 -> 원래의 처리 실패 원인을 기록
  @Override
  public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
    increment(record.topic(), ex);
  }

  // 재시도 소진 후 DLT 발행까지 실패 -> DLT 발행 실패 원인을 기록
  @Override
  public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
    increment(record.topic(), failure);
  }

  private void increment(String topic, Throwable cause) {
    Counter.builder(FAILURE_COUNTER)
        .description(DESCRIPTION)
        .tag(TAG_TOPIC, topic)
        .tag(TAG_REASON, reasonOf(cause))
        .register(meterRegistry)
        .increment();
  }

  private String reasonOf(Throwable cause) {
    if (cause == null) {
      return UNKNOWN_REASON;
    }

    return NestedExceptionUtils.getMostSpecificCause(cause).getClass().getSimpleName();
  }
}