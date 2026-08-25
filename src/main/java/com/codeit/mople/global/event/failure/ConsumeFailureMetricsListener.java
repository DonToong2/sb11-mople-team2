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

// 소비 최종 실패는 Kafka 클라이언트 메트릭에 대응물이 없어(애플리케이션 레벨 사건) 직접 센다.
// 발행 실패는 빌트인 kafka.producer.record.error.total 로 본다.
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

  // 예외 메시지를 태그로 쓰면 시계열이 무한히 늘어나므로 클래스 이름만 쓴다.
  // ListenerExecutionFailedException 으로 감싸여 오므로 가장 안쪽 원인까지 벗긴다
  private String reasonOf(Throwable cause) {
    if (cause == null) {
      return UNKNOWN_REASON;
    }

    return NestedExceptionUtils.getMostSpecificCause(cause).getClass().getSimpleName();
  }
}