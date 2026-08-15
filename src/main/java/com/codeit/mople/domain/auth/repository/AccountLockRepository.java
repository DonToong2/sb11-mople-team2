package com.codeit.mople.domain.auth.repository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountLockRepository {

  private static final String KEY_PREFIX = "account:locked:";

  private final RedisTemplate<String, Object> redisTemplate;

  public void lock(UUID userId) {
    redisTemplate.opsForValue().set(KEY_PREFIX + userId, Boolean.TRUE);
  }

  public void unlock(UUID userId) {
    redisTemplate.delete(KEY_PREFIX + userId);
  }

  public boolean isLocked(UUID userId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + userId));
  }
}
