package com.codeit.mople.global.event.failure;

import com.codeit.mople.global.config.KafkaProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// producer의 최종 실패를 Redis Stream에 저장하는 역할
@Slf4j
@Component
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class RedisFailedEventStore implements FailedEventStore {

  // Redis 키 뒷부분
  private static final String KEY_SUFFIX = ":kafka:events:failed";
  // 최대 보관 건수
  private static final long MAX_ENTRIES = 10_000L;
  // 발행 실패 타입
  private static final String TYPE_PRODUCER = "PRODUCER";

  private static final String FIELD_TYPE = "type";
  private static final String FIELD_TOPIC = "topic";
  private static final String FIELD_KEY = "key";
  private static final String FIELD_EVENT_ID = "eventId";
  private static final String FIELD_EVENT_TYPE = "eventType";
  private static final String FIELD_DATA = "data";
  private static final String FIELD_ERROR = "error";

  // redis 서버에 보내주는 통로
  private final StringRedisTemplate redisTemplate;
  private final String streamKey;

  public RedisFailedEventStore(
      StringRedisTemplate redisTemplate,
      @Value("${redis.namespace}") String namespace) {
    this.redisTemplate = redisTemplate;
    this.streamKey = namespace + KEY_SUFFIX;
  }

  @Override
  public void save(FailedEvent event) {
    try {
      // streamKey: 해당스트림에
      // toFields: 뭘 저장할지
      // sizeLimitOptions(): 저장하면서 적용할 옵션
      redisTemplate.opsForStream().add(streamKey, toFields(event), sizeLimitOptions());
    } catch (Exception e) {
      log.error("실패 이벤트 적재 실패: topic={}, eventId={}, eventType={}",
          event.topic(), event.eventId(), event.eventType(), e);
    }
  }

  // 기간을 넘긴 항목은 애초에 조회되지 않게 해서, 오래된 이벤트를 되살리는 실수를 막음
  @Override
  public List<FailedEvent> findRecent(Duration within, int limit) {
    String fromId = (System.currentTimeMillis() - within.toMillis()) + "-0";

    List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(
        streamKey,
        Range.rightUnbounded(Range.Bound.inclusive(fromId)),
        Limit.limit().count(limit)
    );

    if (records == null) {
      return List.of();
    }

    return records.stream().map(this::toFailedEvent).toList();
  }

  @Override
  public void delete(String recordId) {
    redisTemplate.opsForStream().delete(streamKey, recordId);
  }

  // stream에 저장할 내용
  private Map<String, String> toFields(FailedEvent event) {
    return Map.of(
        FIELD_TYPE, TYPE_PRODUCER,
        FIELD_TOPIC, event.topic(),
        FIELD_KEY, event.key(),
        FIELD_EVENT_ID, String.valueOf(event.eventId()),
        FIELD_EVENT_TYPE, event.eventType(),
        FIELD_DATA, event.data(),
        FIELD_ERROR, event.error()
    );
  }

  private FailedEvent toFailedEvent(MapRecord<String, Object, Object> record) {
    Map<Object, Object> fields = record.getValue();

    return new FailedEvent(
        record.getId().getValue(),
        valueOf(fields, FIELD_TOPIC),
        valueOf(fields, FIELD_KEY),
        UUID.fromString(valueOf(fields, FIELD_EVENT_ID)),
        valueOf(fields, FIELD_EVENT_TYPE),
        valueOf(fields, FIELD_DATA),
        valueOf(fields, FIELD_ERROR)
    );
  }

  private String valueOf(Map<Object, Object> fields, String field) {
    Object value = fields.get(field);

    return value == null ? "" : value.toString();
  }

  // 최대 보관 건수를 넘으면 오래된 것부터 잘라라.(대략 근사치로 잘라라)
  private XAddOptions sizeLimitOptions() {
    return XAddOptions.maxlen(MAX_ENTRIES).approximateTrimming(true);
  }
}