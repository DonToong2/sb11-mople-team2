package com.codeit.mople.global.event.failure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.mople.global.event.failure.FailedEventQuery.Order;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedEventsEndpoint 테스트")
class FailedEventsEndpointTest {

  static final String TOPIC = "mople.follow.created.v1";

  @InjectMocks
  FailedEventsEndpoint endpoint;

  @Mock
  FailedEventStore failedEventStore;
  @Mock
  FailedEventReplayer replayer;

  @Captor
  ArgumentCaptor<FailedEventQuery> queryCaptor;

  FailedEventQuery listQuery(Integer withinHours, String topic, Integer limit) {
    given(failedEventStore.find(any(FailedEventQuery.class))).willReturn(List.of());

    endpoint.list(withinHours, topic, limit);

    verify(failedEventStore).find(queryCaptor.capture());

    return queryCaptor.getValue();
  }

  @Nested
  @DisplayName("실패 이벤트 조회")
  class ListOperation {

    @Test
    @DisplayName("아무것도 안 넘기면 최근 24시간을 최신순으로 조회하는지")
    void listsNewestFirst() {
      // when
      FailedEventQuery query = listQuery(null, null, null);

      // then
      assertThat(query.order()).isEqualTo(Order.NEWEST_FIRST);
      assertThat(query.within()).isEqualTo(Duration.ofHours(24));
      assertThat(query.limit()).isEqualTo(100);
      assertThat(query.hasTopicFilter()).isFalse();
    }

    @Test
    @DisplayName("토픽을 집어 넣으면 그 토픽만 조회 조건에 실리는지")
    void listsByTopic() {
      // when
      FailedEventQuery query = listQuery(null, TOPIC, null);

      // then
      assertThat(query.topic()).isEqualTo(TOPIC);
      assertThat(query.hasTopicFilter()).isTrue();
    }

    @Test
    @DisplayName("사용자 정보가 든 본문을 빼고 목록을 내려주는지")
    void listsWithoutBody() {
      // given
      UUID eventId = UUID.randomUUID();
      FailedEvent failed = new FailedEvent(
          "1756000000000-0", TOPIC, "followee-key", eventId,
          "com.codeit.mople.domain.follow.event.FollowCreatedMessage",
          "{\"followId\":\"test\"}", "broker down");

      given(failedEventStore.find(any(FailedEventQuery.class))).willReturn(List.of(failed));

      // when
      List<FailedEventsEndpoint.FailedEventSummary> summaries = endpoint.list(null, null, null);

      // then
      assertThat(summaries).singleElement().satisfies(summary -> {
        assertThat(summary.recordId()).isEqualTo("1756000000000-0");
        assertThat(summary.topic()).isEqualTo(TOPIC);
        assertThat(summary.eventId()).isEqualTo(eventId);
        assertThat(summary.replayable()).isTrue();
      });
    }
  }

  @Nested
  @DisplayName("실패 이벤트 재발행")
  class ReplayOperation {

    @Test
    @DisplayName("재발행은 원래 순서를 지키도록 오래된 순으로 나가는지")
    void replaysOldestFirst() {
      // when
      endpoint.replay(null, TOPIC, null);

      // then
      verify(replayer).replay(queryCaptor.capture());

      assertThat(queryCaptor.getValue().order()).isEqualTo(Order.OLDEST_FIRST);
      assertThat(queryCaptor.getValue().topic()).isEqualTo(TOPIC);
    }
  }

  @Nested
  @DisplayName("입력 방어")
  class Bounds {

    @Test
    @DisplayName("조회 건수에 음수를 집어 넣으면 1로 올려서 상한 없는 조회를 막는지")
    void clampsNonPositiveLimit() {
      // when
      FailedEventQuery query = listQuery(null, null, -1);

      // then
      assertThat(query.limit()).isEqualTo(1);
    }

    @Test
    @DisplayName("조회 건수가 상한을 넘으면 상한으로 내려주는지")
    void clampsLimitToMax() {
      // when
      FailedEventQuery query = listQuery(null, null, 5000);

      // then
      assertThat(query.limit()).isEqualTo(1000);
    }

    @Test
    @DisplayName("조회 기간에 0을 집어 넣으면 1시간으로 올려서 잘못된 스트림 ID를 막는지")
    void clampsNonPositiveWithinHours() {
      // when
      FailedEventQuery query = listQuery(0, null, null);

      // then
      assertThat(query.within()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("조회 기간이 보관 기간을 넘으면 7일로 내려주는지")
    void clampsWithinHoursToMax() {
      // when
      FailedEventQuery query = listQuery(Integer.MAX_VALUE, null, null);

      // then
      assertThat(query.within()).isEqualTo(Duration.ofDays(7));
    }
  }
}