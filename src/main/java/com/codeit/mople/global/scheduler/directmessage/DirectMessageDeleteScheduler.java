package com.codeit.mople.global.scheduler.directmessage;

import com.codeit.mople.domain.directmessage.repository.DirectMessageSearchRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!load")
@Component
@RequiredArgsConstructor
public class DirectMessageDeleteScheduler {

  private final DirectMessageSearchRepository directMessageSearchRepository;

  @Value("${mople.elasticsearch.dm-retention-days:365}")
  private int retentionDays;

  @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
  @SchedulerLock(name = "dm-es-document-cleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
  public void  cleanupOldSearchData() {
    log.info("ES 대화방 검색 데이터 만료: 정리 배치 시작 (보관 주기: {}일)", retentionDays);

    // 1년 전 시간 계산
    Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

    try {
      directMessageSearchRepository.deleteByCreatedAtBefore(cutoffDate);
      log.info("ES 대화방 검색 데이터 만료 정리 완료: - 기준일 {} 이전 데이터 삭제", cutoffDate);
    } catch (Exception e) {
      log.error("ES 대화방 검색 데이터 만료 정리 중 에러 발생", e);
    }
  }
}
