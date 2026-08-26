package com.codeit.mople.global.event.failure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.mople.global.event.PublishableEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedEventReplayer 테스트")
class FailedEventReplayerTest {

  static final String TOPIC = "mople.follow.created.v1";
  static final String KEY = "followee-key";
  static final String RECORD_ID = "1756000000000-0";
  static final Duration WITHIN = Duration.ofHours(24);
  static final int LIMIT = 100;
  static final FailedEventQuery QUERY = FailedEventQuery.oldestFirst(WITHIN, null, LIMIT);

  public record ReplayTestEvent(UUID eventId) implements PublishableEvent {}

  @Mock
  FailedEventStore failedEventStore;
  @Mock
  KafkaTemplate<String, Object> kafkaTemplate;

  @Captor
  ArgumentCaptor<PublishableEvent> eventCaptor;

  ObjectMapper objectMapper;
  FailedEventReplayer replayer;

  UUID eventId;
  String data;

  @BeforeEach
  void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    replayer = new FailedEventReplayer(failedEventStore, kafkaTemplate, objectMapper);

    eventId = UUID.randomUUID();
    data = objectMapper.writeValueAsString(new ReplayTestEvent(eventId));
  }

  FailedEvent failedEvent(String key, String eventType, String body) {
    return new FailedEvent(RECORD_ID, TOPIC, key, eventId, eventType, body, "broker down");
  }

  FailedEvent replayable(String recordId) {
    return new FailedEvent(
        recordId, TOPIC, KEY, eventId, ReplayTestEvent.class.getName(), data, "broker down");
  }

  CompletableFuture<SendResult<String, Object>> delivered() {
    return CompletableFuture.completedFuture(null);
  }

  CompletableFuture<SendResult<String, Object>> rejected() {
    return CompletableFuture.failedFuture(new IllegalStateException("broker down"));
  }

  @Nested
  @DisplayName("실패 이벤트 재발행")
  class Replay {

    @Test
    @DisplayName("저장해둔 본문을 원래 이벤트 타입으로 되살려서 같은 토픽으로 다시 보내고 원본까지 지우는지")
    void replaySuccess() {
      // given
      given(failedEventStore.find(QUERY))
          .willReturn(List.of(failedEvent(KEY, ReplayTestEvent.class.getName(), data)));
      given(kafkaTemplate.send(eq(TOPIC), eq(KEY), any(PublishableEvent.class)))
          .willReturn(delivered());

      // when
      FailedEventReplayResult result = replayer.replay(QUERY);

      // then
      verify(kafkaTemplate).send(eq(TOPIC), eq(KEY), eventCaptor.capture());
      verify(failedEventStore).delete(RECORD_ID);

      assertThat(eventCaptor.getValue()).isInstanceOf(ReplayTestEvent.class);
      assertThat(eventCaptor.getValue().eventId()).isEqualTo(eventId);
      assertThat(result).isEqualTo(new FailedEventReplayResult(1, 1, 0, 0));
    }

    @Test
    @DisplayName("재발행해도 eventId가 그대로 유지돼서 소비 쪽에서 중복으로 처리되지 않는지")
    void replayKeepsEventId() {
      // given
      given(failedEventStore.find(QUERY))
          .willReturn(List.of(failedEvent(KEY, ReplayTestEvent.class.getName(), data)));
      given(kafkaTemplate.send(anyString(), anyString(), any(PublishableEvent.class)))
          .willReturn(delivered());

      // when
      replayer.replay(QUERY);

      // then
      verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

      assertThat(eventCaptor.getValue().eventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("키가 빈 문자열이면 키 없이 보내서 파티셔닝이 원래랑 같아지는지")
    void replayWithoutKey() {
      // given
      given(failedEventStore.find(QUERY))
          .willReturn(List.of(failedEvent("", ReplayTestEvent.class.getName(), data)));
      given(kafkaTemplate.send(eq(TOPIC), isNull(), any(PublishableEvent.class)))
          .willReturn(delivered());

      // when
      replayer.replay(QUERY);

      // then
      verify(kafkaTemplate).send(eq(TOPIC), isNull(), any(PublishableEvent.class));
    }

    @Test
    @DisplayName("본문이 없는 항목을 집어 넣으면 재발행하지 않고 제외로 세는지")
    void skipWhenBodyIsEmpty() {
      // given
      given(failedEventStore.find(QUERY))
          .willReturn(List.of(failedEvent(KEY, ReplayTestEvent.class.getName(), "")));

      // when
      FailedEventReplayResult result = replayer.replay(QUERY);

      // then
      verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
      verify(failedEventStore, never()).delete(anyString());

      assertThat(result).isEqualTo(new FailedEventReplayResult(1, 0, 1, 0));
    }

    @Test
    @DisplayName("우리 패키지 밖의 이벤트 타입을 집어 넣으면 실패로 세고 원본을 남기는지")
    void failWhenEventTypeIsNotTrusted() {
      // given
      given(failedEventStore.find(QUERY))
          .willReturn(List.of(failedEvent(KEY, "java.lang.String", data)));

      // when
      FailedEventReplayResult result = replayer.replay(QUERY);

      // then
      verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
      verify(failedEventStore, never()).delete(anyString());

      assertThat(result).isEqualTo(new FailedEventReplayResult(1, 0, 0, 1));
    }
  }

  @Nested
  @DisplayName("재발행 결과 확정")
  class Confirm {

    @Test
    @DisplayName("브로커가 못 받으면 원본을 안 지우고 실패로 세는지")
    void keepOriginalWhenSendFails() {
      // given
      given(failedEventStore.find(QUERY)).willReturn(List.of(replayable(RECORD_ID)));
      given(kafkaTemplate.send(eq(TOPIC), eq(KEY), any(PublishableEvent.class)))
          .willReturn(rejected());

      // when
      FailedEventReplayResult result = replayer.replay(QUERY);

      // then
      verify(failedEventStore, never()).delete(anyString());

      assertThat(result).isEqualTo(new FailedEventReplayResult(1, 0, 0, 1));
    }

    @Test
    @DisplayName("둘 중 하나만 실패하면 성공한 것만 지우고 나머지는 실패로 세는지")
    void deleteOnlyDeliveredOnes() {
      // given
      FailedEvent first = replayable("1756000000000-0");
      FailedEvent second = replayable("1756000000001-0");

      given(failedEventStore.find(QUERY)).willReturn(List.of(first, second));
      given(kafkaTemplate.send(eq(TOPIC), eq(KEY), any(PublishableEvent.class)))
          .willReturn(delivered(), rejected());

      // when
      FailedEventReplayResult result = replayer.replay(QUERY);

      // then
      verify(failedEventStore).delete("1756000000000-0");
      verify(failedEventStore, never()).delete("1756000000001-0");

      assertThat(result).isEqualTo(new FailedEventReplayResult(2, 1, 0, 1));
    }

    @Test
    @DisplayName("원본 삭제가 실패해도 중간에 멈추지 않고 나머지를 계속 처리하는지")
    void continueWhenDeleteFails() {
      // given
      given(failedEventStore.find(QUERY))
          .willReturn(List.of(replayable("1756000000000-0"), replayable("1756000000001-0")));
      given(kafkaTemplate.send(eq(TOPIC), eq(KEY), any(PublishableEvent.class)))
          .willReturn(delivered(), delivered());
      willThrow(new IllegalStateException("redis down"))
          .given(failedEventStore).delete("1756000000000-0");

      // when
      FailedEventReplayResult result = replayer.replay(QUERY);

      // then
      verify(failedEventStore).delete("1756000000001-0");

      assertThat(result).isEqualTo(new FailedEventReplayResult(2, 2, 0, 0));
    }

    @Test
    @DisplayName("앞 건 결과를 기다리지 않고 대상 전체를 먼저 보내는지")
    void dispatchesAllBeforeAwaiting() {
      // given
      AtomicInteger sent = new AtomicInteger();
      CompletableFuture<SendResult<String, Object>> pending = new CompletableFuture<>();

      given(failedEventStore.find(QUERY))
          .willReturn(List.of(replayable("1756000000000-0"), replayable("1756000000001-0")));
      given(kafkaTemplate.send(eq(TOPIC), eq(KEY), any(PublishableEvent.class)))
          .willAnswer(invocation -> {
            if (sent.getAndIncrement() == 0) {
              return pending;
            }

            pending.complete(null);

            return delivered();
          });

      // when
      FailedEventReplayResult result = replayer.replay(QUERY);

      // then
      assertThat(result).isEqualTo(new FailedEventReplayResult(2, 2, 0, 0));
    }
  }
}