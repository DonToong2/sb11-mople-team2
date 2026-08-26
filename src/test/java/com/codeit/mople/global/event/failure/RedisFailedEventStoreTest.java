package com.codeit.mople.global.event.failure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisFailedEventStore 테스트")
class RedisFailedEventStoreTest {

  static final String NAMESPACE = "mople:test";
  static final String STREAM_KEY = "mople:test:kafka:events:failed";
  static final int MAX_ENTRIES = 10_000;
  static final String EVENT_TYPE = "com.codeit.mople.domain.follow.event.FollowCreatedMessage";
  static final Duration WITHIN = Duration.ofHours(24);

  @Mock
  StringRedisTemplate redisTemplate;
  @Mock
  StreamOperations<String, Object, Object> streamOperations;

  @Captor
  ArgumentCaptor<XAddOptions> optionsCaptor;
  @Captor
  ArgumentCaptor<Map<String, String>> fieldsCaptor;
  @Captor
  ArgumentCaptor<Range<String>> rangeCaptor;
  @Captor
  ArgumentCaptor<Limit> limitCaptor;

  RedisFailedEventStore store;

  UUID eventId;
  FailedEvent event;

  @BeforeEach
  void setUp() {
    store = new RedisFailedEventStore(redisTemplate, NAMESPACE);
    eventId = UUID.randomUUID();
    event = new FailedEvent(
        null,
        "mople.follow.created.v1",
        "followee-key",
        eventId,
        EVENT_TYPE,
        "{\"followId\":\"test\"}",
        "broker down"
    );
  }

  @Nested
  @DisplayName("발행 실패 이벤트 적재")
  class Save {

    @Test
    @DisplayName("프로파일 네임스페이스를 붙인 스트림 키에 이벤트가 제대로 쌓이는지")
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
              "eventType", EVENT_TYPE,
              "data", "{\"followId\":\"test\"}",
              "error", "broker down"
          )),
          any(XAddOptions.class)
      );
    }

    @Test
    @DisplayName("적재할 때마다 근사 크기 상한 옵션이 같이 넘어가는지")
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
    @DisplayName("키가 없는 이벤트를 집어 넣으면 빈 문자열로 쌓이는지")
    void saveWhenKeyIsEmpty() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      FailedEvent noKey = new FailedEvent(
          null,
          "mople.notification.created.v1",
          "",
          eventId,
          EVENT_TYPE,
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
    @DisplayName("Redis가 죽어 있어도 예외를 밖으로 던지지 않는지")
    void saveWhenRedisDown() {
      // given
      given(redisTemplate.opsForStream())
          .willThrow(new RedisConnectionFailureException("redis down"));

      // when & then
      assertThatCode(() -> store.save(event)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("발행 실패 이벤트 조회")
  class Find {

    @Test
    @DisplayName("최신순으로 요청하면 XREVRANGE로 거슬러 올라가며 읽는지")
    void findNewestFirst() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.reverseRange(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(List.of());

      // when
      store.find(FailedEventQuery.newestFirst(WITHIN, null, 100));

      // then
      verify(streamOperations)
          .reverseRange(eq(STREAM_KEY), rangeCaptor.capture(), limitCaptor.capture());
      verify(streamOperations, never()).range(anyString(), any(Range.class), any(Limit.class));

      assertThat(rangeCaptor.getValue().getLowerBound().isBounded()).isTrue();
      assertThat(rangeCaptor.getValue().getUpperBound().isBounded()).isFalse();
      assertThat(limitCaptor.getValue().getCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("오래된 순으로 요청하면 XRANGE로 기간 시작점부터 읽는지")
    void findOldestFirst() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.range(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(List.of());

      // when
      store.find(FailedEventQuery.oldestFirst(WITHIN, null, 100));

      // then
      verify(streamOperations).range(eq(STREAM_KEY), any(Range.class), any(Limit.class));
      verify(streamOperations, never())
          .reverseRange(anyString(), any(Range.class), any(Limit.class));
    }

    @Test
    @DisplayName("조회 기간을 집어 넣으면 현재 시각에서 그만큼 뺀 스트림 ID부터 읽는지")
    void findFromWindowStart() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.range(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(List.of());

      long before = System.currentTimeMillis() - WITHIN.toMillis();

      // when
      store.find(FailedEventQuery.oldestFirst(WITHIN, null, 100));

      // then
      verify(streamOperations).range(eq(STREAM_KEY), rangeCaptor.capture(), any(Limit.class));

      long after = System.currentTimeMillis() - WITHIN.toMillis();
      String fromId = rangeCaptor.getValue().getLowerBound().getValue().orElseThrow();

      assertThat(fromId).endsWith("-0");
      assertThat(Long.parseLong(fromId.substring(0, fromId.indexOf('-'))))
          .isBetween(before, after);
    }

    @Test
    @DisplayName("토픽을 집어 넣으면 보관 상한까지 훑어서 그 토픽만 남기는지")
    void findByTopic() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.range(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(List.of(
              record("1756000000000-0", "mople.follow.created.v1"),
              record("1756000000001-0", "mople.notification.created.v1"),
              record("1756000000002-0", "mople.follow.created.v1")
          ));

      // when
      List<FailedEvent> found =
          store.find(FailedEventQuery.oldestFirst(WITHIN, "mople.follow.created.v1", 100));

      // then
      verify(streamOperations).range(eq(STREAM_KEY), any(Range.class), limitCaptor.capture());

      assertThat(limitCaptor.getValue().getCount()).isEqualTo(MAX_ENTRIES);
      assertThat(found).extracting(FailedEvent::recordId)
          .containsExactly("1756000000000-0", "1756000000002-0");
    }

    @Test
    @DisplayName("토픽으로 거르고 난 뒤에 조회 건수가 적용되는지")
    void findByTopicAppliesLimitAfterFiltering() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.range(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(List.of(
              record("1756000000000-0", "mople.notification.created.v1"),
              record("1756000000001-0", "mople.follow.created.v1"),
              record("1756000000002-0", "mople.follow.created.v1")
          ));

      // when
      List<FailedEvent> found =
          store.find(FailedEventQuery.oldestFirst(WITHIN, "mople.follow.created.v1", 1));

      // then
      assertThat(found).extracting(FailedEvent::recordId).containsExactly("1756000000001-0");
    }

    @Test
    @DisplayName("스트림에 저장된 필드가 실패 이벤트로 잘 복원되는지")
    void findMapsFields() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.range(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(List.of(record("1756000000000-0", "mople.follow.created.v1")));

      // when
      List<FailedEvent> found = store.find(FailedEventQuery.oldestFirst(WITHIN, null, 100));

      // then
      assertThat(found).singleElement().satisfies(failed -> {
        assertThat(failed.recordId()).isEqualTo("1756000000000-0");
        assertThat(failed.topic()).isEqualTo("mople.follow.created.v1");
        assertThat(failed.key()).isEqualTo("followee-key");
        assertThat(failed.eventId()).isEqualTo(eventId);
        assertThat(failed.eventType()).isEqualTo(EVENT_TYPE);
        assertThat(failed.data()).isEqualTo("{\"followId\":\"test\"}");
        assertThat(failed.error()).isEqualTo("broker down");
        assertThat(failed.replayable()).isTrue();
      });
    }

    @Test
    @DisplayName("형태가 깨진 레코드가 섞여 있어도 나머지는 잘 뽑아내는지")
    void findSkipsMalformedRecord() {
      // given
      Map<Object, Object> broken = new LinkedHashMap<>();
      broken.put("topic", "mople.follow.created.v1");
      broken.put("eventId", "uuid-가-아님");

      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.range(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(List.of(
              MapRecord.create(STREAM_KEY, broken).withId(RecordId.of("1756000000000-0")),
              record("1756000000001-0", "mople.follow.created.v1")
          ));

      // when
      List<FailedEvent> found = store.find(FailedEventQuery.oldestFirst(WITHIN, null, 100));

      // then
      assertThat(found).extracting(FailedEvent::recordId).containsExactly("1756000000001-0");
    }

    @Test
    @DisplayName("스트림이 없어서 null이 와도 빈 목록을 돌려주는지")
    void findWhenStreamIsMissing() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);
      given(streamOperations.range(eq(STREAM_KEY), any(Range.class), any(Limit.class)))
          .willReturn(null);

      // when
      List<FailedEvent> found = store.find(FailedEventQuery.oldestFirst(WITHIN, null, 100));

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("발행 실패 이벤트 삭제")
  class Delete {

    @Test
    @DisplayName("프로파일 네임스페이스를 붙인 스트림 키에서 레코드를 지우는지")
    void deleteByRecordId() {
      // given
      given(redisTemplate.opsForStream()).willReturn(streamOperations);

      // when
      store.delete("1756000000000-0");

      // then
      verify(streamOperations).delete(STREAM_KEY, "1756000000000-0");
    }
  }

  MapRecord<String, Object, Object> record(String recordId, String topic) {
    Map<Object, Object> fields = new LinkedHashMap<>();

    fields.put("type", "PRODUCER");
    fields.put("topic", topic);
    fields.put("key", "followee-key");
    fields.put("eventId", eventId.toString());
    fields.put("eventType", EVENT_TYPE);
    fields.put("data", "{\"followId\":\"test\"}");
    fields.put("error", "broker down");

    return MapRecord.create(STREAM_KEY, fields).withId(RecordId.of(recordId));
  }
}