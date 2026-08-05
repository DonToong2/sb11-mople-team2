package com.codeit.mople.domain.content.client.sportsdb.batch;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SportsBatchMetrics {

  private final Counter successCounter;
  private final Counter failureCounter;

  //Micrometer 기반 커스텀 카운터 메트릭 등록
  public SportsBatchMetrics(MeterRegistry meterRegistry) {
    this.successCounter = Counter.builder("batch.sports.success")
        .description("SportsDB 수집 배치 성공 횟수")
        .register(meterRegistry);
    this.failureCounter = Counter.builder("batch.sports.failure")
        .description("SportsDB 수집 배치 실패 횟수")
        .register(meterRegistry);
  }

  public void incrementSuccess() {
    successCounter.increment();
  }

  public void incrementFailure() {
    failureCounter.increment();
  }
}
