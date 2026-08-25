package com.codeit.mople.global.event.failure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.mople.global.event.KafkaEventPublisher;
import com.codeit.mople.global.event.PublishableEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FailedEventReplayerTest {

  static final String TOPIC = "mople.follow.created.v1";
  static final String KEY = "followee-key";
  static final String RECORD_ID = "1756000000000-0";
  static final Duration WITHIN = Duration.ofHours(24);
  static final int LIMIT = 100;

  public record ReplayTestEvent(UUID eventId) implements PublishableEvent {}

  @Mock
  FailedEventStore failedEventStore;
  @Mock
  KafkaEventPublisher publisher;

  @Captor
  ArgumentCaptor<PublishableEvent> eventCaptor;

  ObjectMapper objectMapper;
  FailedEventReplayer replayer;

  UUID eventId;
  String data;

  @BeforeEach
  void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    replayer = new FailedEventReplayer(failedEventStore, publisher, objectMapper);

    eventId = UUID.randomUUID();
    data = objectMapper.writeValueAsString(new ReplayTestEvent(eventId));
  }

  FailedEvent failedEvent(String key, String eventType, String body) {
    return new FailedEvent(RECORD_ID, TOPIC, key, eventId, eventType, body, "broker down");
  }

  @Nested
  @DisplayName("실패 이벤트 재발행")
  class Replay {

    @Test
    @DisplayName("본문을 원래 이벤트 타입으로 되살려 같은 토픽에 재발행하고 원본을 삭제")
    void replaySuccess() {
      // given
      given(failedEventStore.findRecent(WITHIN, LIMIT))
          .willReturn(List.of(failedEvent(KEY, ReplayTestEvent.class.getName(), data)));

      // when
      FailedEventReplayResult result = replayer.replay(WITHIN, LIMIT);

      // then
      verify(publisher).publish(eq(TOPIC), eq(KEY), eventCaptor.capture());
      verify(failedEventStore).delete(RECORD_ID);

      assertThat(eventCaptor.getValue()).isInstanceOf(ReplayTestEvent.class);
      assertThat(eventCaptor.getValue().eventId()).isEqualTo(eventId);
      assertThat(result).isEqualTo(new FailedEventReplayResult(1, 1, 0, 0));
    }

    @Test
    @DisplayName("재발행해도 이벤트 식별자가 유지되어 소비 쪽 중복 처리가 막힘")
    void replayKeepsEventId() {
      // given
      given(failedEventStore.findRecent(WITHIN, LIMIT))
          .willReturn(List.of(failedEvent(KEY, ReplayTestEvent.class.getName(), data)));

      // when
      replayer.replay(WITHIN, LIMIT);

      // then
      verify(publisher).publish(anyString(), anyString(), eventCaptor.capture());

      assertThat(eventCaptor.getValue().eventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("키가 빈 문자열이면 키 없이 발행해 파티셔닝이 원래와 같아짐")
    void replayWithoutKey() {
      // given
      given(failedEventStore.findRecent(WITHIN, LIMIT))
          .willReturn(List.of(failedEvent("", ReplayTestEvent.class.getName(), data)));

      // when
      replayer.replay(WITHIN, LIMIT);

      // then
      verify(publisher).publish(eq(TOPIC), isNull(), any(PublishableEvent.class));
    }

    @Test
    @DisplayName("본문이 없는 항목은 재발행하지 않고 제외로 집계")
    void skipWhenBodyIsEmpty() {
      // given
      given(failedEventStore.findRecent(WITHIN, LIMIT))
          .willReturn(List.of(failedEvent(KEY, ReplayTestEvent.class.getName(), "")));

      // when
      FailedEventReplayResult result = replayer.replay(WITHIN, LIMIT);

      // then
      verify(publisher, never()).publish(anyString(), anyString(), any());
      verify(failedEventStore, never()).delete(anyString());

      assertThat(result).isEqualTo(new FailedEventReplayResult(1, 0, 1, 0));
    }

    @Test
    @DisplayName("우리 패키지 밖의 이벤트 타입은 실패로 집계하고 원본을 남김")
    void failWhenEventTypeIsNotTrusted() {
      // given
      given(failedEventStore.findRecent(WITHIN, LIMIT))
          .willReturn(List.of(failedEvent(KEY, "java.lang.String", data)));

      // when
      FailedEventReplayResult result = replayer.replay(WITHIN, LIMIT);

      // then
      verify(publisher, never()).publish(anyString(), anyString(), any());
      verify(failedEventStore, never()).delete(anyString());

      assertThat(result).isEqualTo(new FailedEventReplayResult(1, 0, 0, 1));
    }
  }
}