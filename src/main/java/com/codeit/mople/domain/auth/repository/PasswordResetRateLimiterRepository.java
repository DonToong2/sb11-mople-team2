package com.codeit.mople.domain.auth.repository;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetRateLimiterRepository {

  private static final String KEY_PREFIX = "reset-password:rate-limit:";

  private final RedisTemplate<String, Object> redisTemplate;

  public boolean tryAcquired(String email, Duration ttl) {
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + email, 1, ttl);
    return Boolean.TRUE.equals(acquired);
  }
}
