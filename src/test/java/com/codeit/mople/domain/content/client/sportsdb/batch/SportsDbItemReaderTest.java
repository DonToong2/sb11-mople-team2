package com.codeit.mople.domain.content.client.sportsdb.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.mople.domain.content.client.sportsdb.SportsDbFeignClient;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventDto;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SportsDbItemReaderTest {

  @Mock
  private SportsDbFeignClient feignClient;

  @InjectMocks
  private SportsDbItemReader reader;

  @Test
  @DisplayName("API 통신 성공 시 목록을 하나씩 반환하고 모두 읽으면 null을 반환한다")
  void read_Success_ReturnsItemsSequentially() {
    SportsDbEventDto dto1 = mock(SportsDbEventDto.class);
    SportsDbEventDto dto2 = mock(SportsDbEventDto.class);
    SportsDbEventResponse response = new SportsDbEventResponse(List.of(dto1, dto2));

    //Feign Client 호출 시 2개의 항목이 담긴 응답 반환 설정
    given(feignClient.getEventsByDate(anyString(), eq("Soccer"))).willReturn(response);

    //첫 번째 호출: API 통신 수행 후 첫 번째 아이템 반환
    SportsDbEventDto result1 = reader.read();
    assertThat(result1).isEqualTo(dto1);

    //두 번째 호출: 메모리에서 두 번째 아이템 반환(API 호출 안 함)
    SportsDbEventDto result2 = reader.read();
    assertThat(result2).isEqualTo(dto2);

    //세 번째 호출: 데이터가 모두 소진되었으므로 null 반환
    SportsDbEventDto result3 = reader.read();
    assertThat(result3).isNull();

    //API 통신은 단 1번만 일어났는지 검증
    verify(feignClient, times(1)).getEventsByDate(anyString(), eq("Soccer"));
  }

  @Test
  @DisplayName("API 응답이 null이거나 events 목록이 null이면 IllegalStateException 예외가 발생한다")
  void read_Fail_WhenResponseOrEventsIsNull() {
    // events가 null인 비정상 응답 설정
    SportsDbEventResponse invalidResponse = new SportsDbEventResponse(null);
    given(feignClient.getEventsByDate(anyString(), eq("Soccer"))).willReturn(invalidResponse);

    // 비정상 응답 수신 시 예외 발생 검증
    assertThatThrownBy(() -> reader.read())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("외부 API 응답이 누락되었거나 유효하지 않습니다.");
  }

  @Test
  @DisplayName("events 목록이 빈 리스트인 경우 정상적으로 null을 반환한다 (빈 경기일)")
  void read_Success_WhenEventsIsEmpty() {
    // events가 빈 리스트인 정상 응답 설정 (경기가 없는 날)
    SportsDbEventResponse emptyResponse = new SportsDbEventResponse(List.of());
    given(feignClient.getEventsByDate(anyString(), eq("Soccer"))).willReturn(emptyResponse);

    SportsDbEventDto result = reader.read();

    assertThat(result).isNull();
  }
}