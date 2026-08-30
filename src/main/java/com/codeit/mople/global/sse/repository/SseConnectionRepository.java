package com.codeit.mople.global.sse.repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SseConnectionRepository {

  private static final String KEY_PREFIX = "sse:connection:";
  private static final Duration TTL = Duration.ofHours(2);

  private final StringRedisTemplate redisTemplate;

  private final RedisScript<Long> removeIfMatch = RedisScript.of(
      """
          if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
          end
          return 0
          """,
      Long.class
  );

  public void save(UUID receiverId, String serverId, UUID connectionId) {
    String value = serverId + ":" + connectionId;

    redisTemplate.opsForValue().set(getKey(receiverId), value, TTL);
  }

  public String findServerId(UUID receiverId) {
    String value = redisTemplate.opsForValue().get(getKey(receiverId));

    if (value == null) {
      return null;
    }

    int separatorIndex = value.indexOf(':');

    // value에 ":"이 포함되어 있지 않으면 전체 value를 반환
    if (separatorIndex == -1) {
      return value;
    }

    return value.substring(0, value.indexOf(':'));
  }

  public void removeIfOwner(UUID receiverId, String serverId, UUID connectionId) {
    String value = serverId + ":" + connectionId;

    redisTemplate.execute(removeIfMatch, List.of(getKey(receiverId)), value);
  }

  private String getKey(UUID receiverId) {
    return KEY_PREFIX + receiverId;
  }

}
