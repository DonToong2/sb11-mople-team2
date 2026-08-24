package com.codeit.mople.global.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.mople.global.event.failure.FailedEvent;
import com.codeit.mople.global.event.failure.FailedEventStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

  static final String TOPIC = "mople.follow.created.v1";
  static final String KEY = "followee-key";

  record TestEvent(UUID eventId) implements PublishableEvent {}

  @InjectMocks
  KafkaEventPublisher publisher;

  @Mock
  KafkaTemplate<String, Object> kafkaTemplate;
  @Mock
  FailedEventStore failedEventStore;
  @Mock
  ObjectMapper objectMapper;

  @Captor
  ArgumentCaptor<FailedEvent> failedEventCaptor;

  TestEvent event;

  @BeforeEach
  void setUp() {
    event = new TestEvent(UUID.randomUUID());
  }

  @Nested
  @DisplayName("이벤트 발행")
  class Publish {

    @Test
    @DisplayName("발행에 성공하면 실패 이벤트를 적재하지 않음")
    void publishSuccess() {
      // given
      CompletableFuture<SendResult<String, Object>> sent =
          CompletableFuture.completedFuture(null);
      given(kafkaTemplate.send(TOPIC, KEY, event)).willReturn(sent);

      // when
      publisher.publish(TOPIC, KEY, event);

      // then
      verify(failedEventStore, never()).save(any());
    }

    @Test
    @DisplayName("발행에 실패하면 토픽, 키, 이벤트 정보를 담아 실패 이벤트를 적재")
    void publishFailWhenSendFails() throws Exception {
      // given
      CompletableFuture<SendResult<String, Object>> failed =
          CompletableFuture.failedFuture(new RuntimeException("broker down"));
      given(kafkaTemplate.send(TOPIC, KEY, event)).willReturn(failed);
      given(objectMapper.writeValueAsString(event)).willReturn("{\"eventId\":\"test\"}");

      // when
      publisher.publish(TOPIC, KEY, event);

      // then
      verify(failedEventStore).save(failedEventCaptor.capture());

      FailedEvent saved = failedEventCaptor.getValue();
      assertThat(saved.topic()).isEqualTo(TOPIC);
      assertThat(saved.key()).isEqualTo(KEY);
      assertThat(saved.eventId()).isEqualTo(event.eventId());
      assertThat(saved.eventType()).isEqualTo("TestEvent");
      assertThat(saved.data()).isEqualTo("{\"eventId\":\"test\"}");
      assertThat(saved.error()).isEqualTo("broker down");
    }

    @Test
    @DisplayName("본문 직렬화에 실패해도 본문 없이 실패 이벤트를 적재")
    void publishFailWhenSerializeFails() throws Exception {
      // given
      CompletableFuture<SendResult<String, Object>> failed =
          CompletableFuture.failedFuture(new RuntimeException("broker down"));
      given(kafkaTemplate.send(TOPIC, KEY, event)).willReturn(failed);
      given(objectMapper.writeValueAsString(event))
          .willThrow(new JsonProcessingException("직렬화 실패") {});

      // when
      publisher.publish(TOPIC, KEY, event);

      // then
      verify(failedEventStore).save(failedEventCaptor.capture());

      FailedEvent saved = failedEventCaptor.getValue();
      assertThat(saved.data()).isEmpty();
      assertThat(saved.eventId()).isEqualTo(event.eventId());
      assertThat(saved.eventType()).isEqualTo("TestEvent");
    }

    @Test
    @DisplayName("발행을 시도하다 예외가 발생해도 실패 이벤트를 적재")
    void publishFailWhenSendThrows() throws Exception {
      // given
      given(kafkaTemplate.send(TOPIC, KEY, event))
          .willThrow(new IllegalStateException("max.block.ms 만료"));
      given(objectMapper.writeValueAsString(event)).willReturn("{\"eventId\":\"test\"}");

      // when
      publisher.publish(TOPIC, KEY, event);

      // then
      verify(failedEventStore).save(failedEventCaptor.capture());
      assertThat(failedEventCaptor.getValue().error()).isEqualTo("max.block.ms 만료");
    }

    @Test
    @DisplayName("키 없이 발행하면 키를 null로 전달")
    void publishWithoutKey() {
      // given
      CompletableFuture<SendResult<String, Object>> sent =
          CompletableFuture.completedFuture(null);
      given(kafkaTemplate.send(TOPIC, (String) null, event)).willReturn(sent);

      // when
      publisher.publish(TOPIC, event);

      // then
      verify(kafkaTemplate).send(TOPIC, (String) null, event);
      verify(failedEventStore, never()).save(any());
    }
  }
}