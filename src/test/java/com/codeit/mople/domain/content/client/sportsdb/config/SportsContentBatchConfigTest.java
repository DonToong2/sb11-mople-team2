package com.codeit.mople.domain.content.client.sportsdb.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.codeit.mople.domain.content.client.sportsdb.SportsDbFeignClient;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventDto;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventResponse;
import com.codeit.mople.domain.content.repository.ContentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@SpringBatchTest
@DisplayName("SportsDB 수집 job 통합 테스트")
class SportsContentBatchConfigTest {

  private static final String RUN_DATE = "2026-08-15";

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private JobRepositoryTestUtils jobRepositoryTestUtils;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  @Qualifier("sportsContentJob")
  private Job sportsContentJob;

  // Reader, Processor 대신 최외곽의 FeignClient(외부 API)만 모킹합니다.
  @MockitoBean
  private SportsDbFeignClient feignClient;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(sportsContentJob);
    jobRepositoryTestUtils.removeJobExecutions();
    contentRepository.deleteAll();

    //외부 API 응답 모킹(가짜 경기 데이터 2건)
    SportsDbEventDto dto1 = createMockDto("EVENT-1", "Team A vs Team B");
    SportsDbEventDto dto2 = createMockDto("EVENT-2", "Team C vs Team D");
    SportsDbEventResponse mockResponse = new SportsDbEventResponse(List.of(dto1, dto2));

    given(feignClient.getEventsByDate(anyString(), eq("Soccer"))).willReturn(mockResponse);
  }

  @Nested
  @DisplayName("Job 실행")
  class Launch {

    @Test
    @DisplayName("삭제 Step이 먼저 돌고 수집 Step이 실행되어 새로운 데이터가 저장된다")
    void launchJob_CompletesAndSaves() throws Exception {
      JobParameters parameters = jobParameters(RUN_DATE);

      JobExecution execution = jobLauncherTestUtils.launchJob(parameters);

      assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

      //사전 삭제 후 2건을 새로 적재했으므로, 최종 데이터는 2건이어야 합니다.
      assertThat(contentRepository.count()).isEqualTo(2);

      //스텝 실행 순서와 처리량 검증(삭제 -> 수집 순서)
      assertThat(execution.getStepExecutions())
          .extracting(StepExecution::getStepName, StepExecution::getReadCount)
          .containsExactly(
              tuple("deleteOldSportsDataStep", 0L), //삭제 스텝 먼저
              tuple("sportsContentStep", 2L)        //수집 스텝 나중
          );
    }

    @Test
    @DisplayName("성공하면 batch.sports.success 카운터가 1 증가한다")
    void launchJob_IncrementsSuccessCounter() throws Exception {
      double before = meterRegistry.get("batch.sports.success").counter().count();

      jobLauncherTestUtils.launchJob(jobParameters(RUN_DATE));

      assertThat(meterRegistry.get("batch.sports.success").counter().count())
          .isEqualTo(before + 1);
    }
  }

  @Nested
  @DisplayName("하루 1회 멱등성")
  class Idempotency {

    @Test
    @DisplayName("같은 파라미터(날짜)로 재실행하면 JobRestartException 예외 발생 (preventRestart 옵션 적용)")
    void launchJob_SameRunDate_AlreadyComplete() throws Exception {
      //1차 실행(성공)
      jobLauncherTestUtils.launchJob(jobParameters(RUN_DATE));

      //2차 실행 시도 시 JobRestartException 발생 검증
      assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(jobParameters(RUN_DATE)))
          .isInstanceOf(JobRestartException.class);
    }
  }

  //JobParameter 생성 헬퍼 메서드
  private JobParameters jobParameters(String runDate) {
    return new JobParametersBuilder()
        .addString("runDate", runDate)
        .toJobParameters();
  }

  //Processor에서 필터링되지 않도록 필수값을 모두 채운 가짜 DTO 생성 헬퍼
  private SportsDbEventDto createMockDto(String id, String title) {
    SportsDbEventDto mockDto = mock(SportsDbEventDto.class);
    given(mockDto.idEvent()).willReturn(id);
    given(mockDto.strEvent()).willReturn(title);
    given(mockDto.dateEvent()).willReturn("2026-08-15");
    given(mockDto.strTime()).willReturn("20:00:00");
    given(mockDto.strHomeTeam()).willReturn("Home");
    given(mockDto.strAwayTeam()).willReturn("Away");
    given(mockDto.strSport()).willReturn("Soccer");
    given(mockDto.strLeague()).willReturn("League");
    given(mockDto.strThumb()).willReturn("thumb.png");
    return mockDto;
  }
}