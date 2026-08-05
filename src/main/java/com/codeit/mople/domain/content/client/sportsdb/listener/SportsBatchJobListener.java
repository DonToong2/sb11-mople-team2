package com.codeit.mople.domain.content.client.sportsdb.listener;

import com.codeit.mople.domain.content.client.sportsdb.batch.SportsBatchMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportsBatchJobListener implements JobExecutionListener {

  private final SportsBatchMetrics metrics;

  //Job 실행 전 시작 시간 및 로깅 기록
  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("SportsDB Batch Job 시작 - 시간: ", jobExecution.getStartTime());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      //Job 종료 후 성공 시 Actuator 성공 메트릭 카운트 증가
      log.info("SportsDB Batch Job 성공적으로 완료");
      metrics.incrementSuccess();
    } else {
      //Job 종료 후 실패 시 Actuator 실패 메트릭 카운트 증가
      log.error("SportsDB Batch Job 실패 - 최종 상태: {}", jobExecution.getStatus());
      metrics.incrementFailure();
    }
  }
}
