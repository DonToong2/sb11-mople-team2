package com.codeit.mople.domain.content.client.sportsdb.config;

import com.codeit.mople.domain.content.client.sportsdb.batch.SportsDbItemProcessor;
import com.codeit.mople.domain.content.client.sportsdb.batch.SportsDbItemReader;
import com.codeit.mople.domain.content.client.sportsdb.batch.SportsDbItemWriter;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventDto;
import com.codeit.mople.domain.content.client.sportsdb.listener.SportsBatchJobListener;
import com.codeit.mople.domain.content.entity.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SportsContentBatchConfig {

  private final SportsDbItemReader reader;
  private final SportsDbItemProcessor processor;
  private final SportsDbItemWriter writer;

  //수집 작업을 총괄하는 Spring Batch Job 구성
  @Bean
  public Job sportsContentJob(
      JobRepository jobRepository,
      Step sportsContentStep,
      SportsBatchJobListener sportsBatchJobListener) {
    return new JobBuilder("sportsContentJob", jobRepository)
        .start(sportsContentStep)
        .listener(sportsBatchJobListener)
        .build();
  }

  //Chunk  기반 Spring Batch Step 구성(read -> process -> write)
  @Bean
  public Step sportsContentStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("sportsContentStep", jobRepository)
        // 한번에 100개씩 데이터를 묶어서(chunk) 처리
        .<SportsDbEventDto, Content>chunk(100, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
  }
}
