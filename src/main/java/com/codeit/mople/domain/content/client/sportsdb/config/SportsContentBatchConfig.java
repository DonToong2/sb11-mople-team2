package com.codeit.mople.domain.content.client.sportsdb.config;

import com.codeit.mople.domain.content.client.sportsdb.batch.SportsDbItemProcessor;
import com.codeit.mople.domain.content.client.sportsdb.batch.SportsDbItemReader;
import com.codeit.mople.domain.content.client.sportsdb.batch.SportsDbItemWriter;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventDto;
import com.codeit.mople.domain.content.client.sportsdb.listener.SportsBatchJobListener;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.ContentRepository;
import feign.FeignException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SportsContentBatchConfig {

  private final SportsDbItemReader itemReader;
  private final SportsDbItemProcessor itemProcessor;
  private final SportsDbItemWriter itemWriter;
  private final SportsBatchJobListener jobListener;
  private final ContentRepository contentRepository;

  //수집 작업을 총괄하는 Spring Batch Job 구성
  @Bean
  public Job sportsContentJob(
      JobRepository jobRepository,
      Step backupOldSportsDataStep,
      Step sportsContentStep,
      Step deleteOldSportsDataStep) {

    //선삭제 흐름 제거, ItemWriter에서 Upsert(갱신)로 처리하여 실패 시 기존 데이터 보존
    return new JobBuilder("sportsContentJob", jobRepository)
        .start(backupOldSportsDataStep)   //기존 데이터 ID 백업 수행
        .next(sportsContentStep)          //신규 데이터 수집 및 Upsert 수행
        .next(deleteOldSportsDataStep)    //신규 수집이 성공한 경우에만 구버전 데이터 삭제 정리
        .listener(jobListener)
        .build();
  }

  //기존 스포츠 데이터를 백업하는 Tasklet Step 추가
  @Bean
  public Step backupOldSportsDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("backupOldSportsDataStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          //externalId가 있는 외부 배치 수집 데이터만 백업 대상으로 지정
          List<UUID> oldIds = contentRepository.findIdsByType(ContentType.SPORT);

          //Job Execution Context에 백업 ID 목록 저장
          chunkContext.getStepContext()
              .getStepExecution()
              .getJobExecution()
              .getExecutionContext()
              .put("oldSportsIds", oldIds);

          log.info("기존 스포츠 데이터 {}건을 백업 목록에 등록했습니다.", oldIds.size());
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  //Chunk 기반 Spring Batch Step 구성(read -> process -> write)
  @Bean
  public Step sportsContentStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    //점점 대기 시간이 길어지는 BackOff 정책 설정
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(2000); //처음 실패 시 2초 대기
    backOffPolicy.setMultiplier(2.0); //다음 실패 시 대기 시간 2배 증가 (2초 -> 4초 -> 8초)
    backOffPolicy.setMaxInterval(10000); //최대 10초까지만 대기

    return new StepBuilder("sportsContentStep", jobRepository)
        //한번에 100개씩 데이터를 묶어서(chunk) 처리
        .<SportsDbEventDto, Content>chunk(100, transactionManager)
        .reader(itemReader)
        .processor(itemProcessor)
        .writer(itemWriter)
        .faultTolerant()
        .retry(FeignException.class) //영구 오류(NPE 등)는 제외하고 API 통신 예외만 재시도
        .retryLimit(3) //최대 3회 재시도
        .backOffPolicy(backOffPolicy) //백오프 정책 적용
        .build();
  }

  //기존 스포츠 데이터를 일괄 삭제하는 Tasklet Step 추가
  @Bean
  @SuppressWarnings("unchecked")
  public Step deleteOldSportsDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("deleteOldSportsDataStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          // 안전하게 신규 데이터를 먼저 적재한 뒤 구버전 데이터를 정리하도록 순서 변경
          List<UUID> oldIds = (List<UUID>) chunkContext.getStepContext()
              .getStepExecution()
              .getJobExecution()
              .getExecutionContext()
              .get("oldSportsIds");

          Set<UUID> processedIds = (Set<UUID>) chunkContext.getStepContext()
              .getStepExecution()
              .getJobExecution()
              .getExecutionContext()
              .get("processedIds");

          if (oldIds != null && !oldIds.isEmpty()) {
            //오늘 수집된 데이터가 있다면 차집합 연산 수행 (processedIds가 비어있다면 오늘 경기가 없는 날이므로 oldIds 전체가 정리 대상이 됨)
            if (processedIds != null && !processedIds.isEmpty()) {
              oldIds.removeAll(processedIds);
            }

            if (!oldIds.isEmpty()) {
              contentRepository.deleteAllById(oldIds);
              log.info("오늘 수집된 데이터를 제외한 구버전 데이터 {}건을 차집합 방식으로 삭제 정리했습니다", oldIds.size());
            } else {
              log.info("삭제할 구버전 데이터가 없습니다 (모든 데이터가 정상 갱신됨)");
            }
          } else {
            log.info("삭제 대상 데이터가 없습니다");
          }

          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }
}