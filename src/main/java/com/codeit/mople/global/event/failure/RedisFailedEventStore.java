package com.codeit.mople.global.event.failure;

import com.codeit.mople.global.config.KafkaProperties;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// producer의 최종 실패를 Redis Stream에 저장하는 역할
@Slf4j
@Component
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class RedisFailedEventStore implements FailedEventStore {

  // Redis 키 뒷부분
  private static final String KEY_SUFFIX = ":kafka:events:failed";
  // 보존 기간 7일
  private static final Duration RETENTION = Duration.ofDays(7);
  // 발행 실패 타입
  private static final String TYPE_PRODUCER = "PRODUCER";
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
      // retentionOptions(): 저장하면서 적용할 옵션
      redisTemplate.opsForStream().add(streamKey, toFields(event), retentionOptions());
    } catch (Exception e) {
      log.error("실패 이벤트 적재 실패: topic={}, eventId={}, eventType={}",
          event.topic(), event.eventId(), event.eventType(), e);
    }
  }

  // stream에 저장할 내용
  private Map<String, String> toFields(FailedEvent event) {
    return Map.of(
        "type", TYPE_PRODUCER,
        "topic", event.topic(),
        "key", event.key(),
        "eventId", String.valueOf(event.eventId()),
        "eventType", event.eventType(),
        "data", event.data(),
        "error", event.error()
    );
  }

  // 지금 시각 - 7일 = 7일전 시각
  private XAddOptions retentionOptions() {
    RecordId retainFrom = RecordId.of(System.currentTimeMillis() - RETENTION.toMillis(), 0);

    // redisStream에 넣을 비어있는 옵션 객체.이 시각/id보다 오래된건 지워라
    return XAddOptions.none().minId(retainFrom);
  }
}
