package com.codeit.mople.global.event.failure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Map;
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
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisFailedEventStoreTest {

  static final String NAMESPACE = "mople:test";
  static final String STREAM_KEY = "mople:test:kafka:events:failed";
  static final long MAX_ENTRIES = 10_000L;

  @Mock
  StringRedisTemplate redisTemplate;
  @Mock
  StreamOperations<String, Object, Object> streamOperations;

  @Captor
  ArgumentCaptor<XAddOptions> optionsCaptor;
  @Captor
  ArgumentCaptor<Map<String, String>> fieldsCaptor;

  RedisFailedEventStore store;

  UUID eventId;
  FailedEvent event;

  @BeforeEach
  void setUp() {
    store = new RedisFailedEventStore(redisTemplate, NAMESPACE);
    eventId = UUID.randomUUID();
    event = new FailedEvent(
        "mople.follow.created.v1",
        "followee-key",
        eventId,
        "FollowCreatedMessage",
        "{\"followId\":\"test\"}",
        "broker down"
    );
  }

  @Nested
  @DisplayName("발행 실패 이벤트 적재")
  class Save {

    @Test
    @DisplayName("프로파일 네임스페이스를 붙인 스트림 키에 이벤트를 적재")
    void saveSuccess() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);

      // when
      store.save(event);

      // then
      verify(streamOperations).add(
          eq(STREAM_KEY),
          eq(Map.of(
              "type", "PRODUCER",
              "topic", "mople.follow.created.v1",
              "key", "followee-key",
              "eventId", eventId.toString(),
              "eventType", "FollowCreatedMessage",
              "data", "{\"followId\":\"test\"}",
              "error", "broker down"
          )),
          any(XAddOptions.class)
      );
    }

    @Test
    @DisplayName("적재할 때마다 근사 크기 상한 옵션만 함께 전달")
    void saveWithSizeLimitOption() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);

      // when
      store.save(event);

      // then
      verify(streamOperations).add(eq(STREAM_KEY), anyMap(), optionsCaptor.capture());

      XAddOptions options = optionsCaptor.getValue();
      assertThat(options.hasMaxlen()).isTrue();
      assertThat(options.getMaxlen()).isEqualTo(MAX_ENTRIES);
      assertThat(options.isApproximateTrimming()).isTrue();
      assertThat(options.hasMinId()).isFalse();
    }

    @Test
    @DisplayName("키가 없는 이벤트도 빈 문자열로 적재")
    void saveWhenKeyIsEmpty() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      FailedEvent noKey = new FailedEvent(
          "mople.notification.created.v1",
          "",
          eventId,
          "NotificationCreatedEvent",
          "{}",
          "broker down"
      );

      // when
      store.save(noKey);

      // then
      verify(streamOperations).add(eq(STREAM_KEY), fieldsCaptor.capture(), any(XAddOptions.class));

      assertThat(fieldsCaptor.getValue()).containsEntry("key", "");
    }

    @Test
    @DisplayName("Redis 적재에 실패해도 예외를 전파하지 않음")
    void saveWhenRedisDown() {
      // given: 적재 실패가 이미 커밋된 발행 요청까지 깨뜨리면 안 됨
      given(redisTemplate.opsForStream())
          .willThrow(new RedisConnectionFailureException("redis down"));

      // when & then
      assertThatCode(() -> store.save(event)).doesNotThrowAnyException();
    }
  }
}