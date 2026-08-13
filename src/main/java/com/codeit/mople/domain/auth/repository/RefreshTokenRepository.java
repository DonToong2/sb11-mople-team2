package com.codeit.mople.domain.auth.repository;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepository {

  private static final String KEY_PREFIX = "refresh:";

  private final RedisTemplate<String, Object> redisTemplate;

  public void save(UUID userId, String refreshToken, Duration ttl) {
    redisTemplate.opsForValue().set(KEY_PREFIX + userId, refreshToken, ttl);
  }

  public boolean isValid(UUID userId, String refreshToken) {
    Object stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
    return refreshToken.equals(stored);
  }

  public void invalidate(UUID userId) {
    redisTemplate.delete(KEY_PREFIX + userId);
  }
}
