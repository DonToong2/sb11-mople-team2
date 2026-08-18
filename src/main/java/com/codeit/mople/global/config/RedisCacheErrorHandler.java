package com.codeit.mople.global.config;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

// CacheErrorHandler -> Redis가 죽어도 API는 돌아가게 함 (읽기/저장 실패는 넘어가고, 무효화 실패만 재시도로 복구)
@Slf4j
@RequiredArgsConstructor
public class RedisCacheErrorHandler implements CacheErrorHandler {

  // 재시도 횟수
  private static final int MAX_ATTEMPTS = 3;
  // 첫번째 재시도를 몇 초 뒤에 할건지
  private static final long FIRST_DELAY_SECONDS = 1L;
  // 다음 재시도 할 때 대기시간 x 5
  private static final int BACKOFF_MULTIPLIER = 5;

  private final ScheduledExecutorService retryScheduler;

  // 캐시에서 데이터를 조회할 때 에러 발생시 호출
  @Override
  public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
    log.warn("캐시 조회에 실패해 원본을 조회합니다: cache={}, key={}", cache.getName(), key, exception);
  }

  // 저장 실패는 다음 조회가 미스로 원본을 보게 되므로 값이 틀리지 않음. 재시도x
  @Override
  public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
    log.warn("캐시 저장에 실패했습니다: cache={}, key={}", cache.getName(), key, exception);
  }

  // 캐시 무효화 실패 -> 재시도
  @Override
  public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
    log.warn("캐시 무효화에 실패했습니다: cache={}, key={}", cache.getName(), key, exception);
    retryEvict(cache, key, 1, FIRST_DELAY_SECONDS);
  }

  // 캐시 전체를 비울 때 에러 발생시 호출
  @Override
  public void handleCacheClearError(RuntimeException exception, Cache cache) {
    log.warn("캐시 전체 삭제에 실패했습니다: cache={}", cache.getName(), exception);
  }

  // 캐시 무효화 재시도 로직
  private void retryEvict(Cache cache, Object key, int attempt, long delaySeconds) {
    retryScheduler.schedule(() -> {
      try {
        cache.evict(key);
        log.info("캐시 무효화 재시도 성공: cache={}, key={}, attempt={}", cache.getName(), key, attempt);
      } catch (RuntimeException e) {
        if (attempt < MAX_ATTEMPTS) {
          retryEvict(cache, key, attempt + 1, delaySeconds * BACKOFF_MULTIPLIER);
        } else {
          log.error("캐시 무효화 재시도를 모두 실패했습니다. TTL 만료까지 옛 값이 남습니다: cache={}, key={}", cache.getName(), key, e);
        }
      }
    }, delaySeconds, TimeUnit.SECONDS);
  }
}