package com.codeit.mople.domain.content.client.sportsdb.config;

import com.codeit.mople.domain.content.client.sportsdb.SportsDbFeignClient;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventDto;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventResponse;
import com.codeit.mople.domain.content.client.sportsdb.listener.SportsBatchJobListener;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.ContentRepository;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SportsContentBatchConfig {

  private final SportsDbFeignClient feignClient;
  private final ContentRepository contentRepository;

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
        .reader(sportsDbItemReader())
        .processor(sportsDbItemProcessor())
        .writer(sportsDbItemWriter())
        .build();
  }

  @Bean
  @StepScope
  public ItemReader<SportsDbEventDto> sportsDbItemReader() {
    return new ItemReader<>() {
      private Iterator<SportsDbEventDto> eventIterator;

      //Feign Client를 호출하여 경기 데이터 읽기 기능 구현
      @Override
      public SportsDbEventDto read() {
        //실행 시 최초 1회만 API를 호출하여 데이터를 메모리에 로드
        if (eventIterator == null) {
          String today = LocalDate.now().toString(); //오늘 날짜 기준 데이터 조회
          log.info("SportsDB API 조회 시작 - 일자: {}", today);
          SportsDbEventResponse response = feignClient.getEventsByDate(today, "Soccer");

          if (response != null && response.events() != null) {
            eventIterator = response.events().iterator();
            log.info("조회된 경기 수: {}건", response.events().size());
          } else {
            eventIterator = List.<SportsDbEventDto>of().iterator();
            log.info("조회된 경기가 없습니다.");
          }
        }
        //Iterator를 통해 Processor로 데이터를 한 건씩 전달(더 없으면 null 반환)
        return eventIterator.hasNext() ? eventIterator.next() : null;
      }
    };
  }

  @Bean
  public ItemProcessor<SportsDbEventDto, Content> sportsDbItemProcessor() {
    //무효한 데이터(필수값 누락) 검증 및 필터링
    return dto -> {
      if (dto.strEvent() == null || dto.dateEvent() == null) {
        log.warn("유효하지 않은 이벤트 데이터 필터링(스킵) - idEvent: {}", dto.idEvent());
        return null; //null을 반환하면 Writer로 넘어가지 않고 스킵
      }
      //외부 DTO를 도메인의 Content 엔티티로 변환
      String title = dto.strEvent();
      String description = String.format("%s vs %s 경기 입니다. 일자: %s, 시간: %s",
          dto.strHomeTeam(), dto.strAwayTeam(), dto.dateEvent(), dto.strTime());
      String thumbnailUrl = dto.strThumb();
      List<String> tags = List.of("Sports", dto.strSport(), dto.strLeague());

      //ContentType은 임시로 SPORTS 사용
      return new Content(ContentType.valueOf("SPORTS"), title, description, thumbnailUrl, tags);
    };
  }

  //변환된 엔티티를 DB에 일괄 저장
  @Bean
  public ItemWriter<Content> sportsDbItemWriter() {
    return chunk -> {
      log.info("Content DB 저장 시작 - Chunk 사이즈: {}건", chunk.getItems().size());
      //Processor를 통과한 유효한 데이터들을 DB에 한 번에 저장
      contentRepository.saveAll(chunk.getItems());
      log.info("Content DB 저장 완료");
    };
  }
}
