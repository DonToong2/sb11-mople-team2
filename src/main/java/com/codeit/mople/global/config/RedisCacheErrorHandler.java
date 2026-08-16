package com.codeit.mople.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

// CacheErrorHandler -> Redis가 죽어도 API는 돌아가게 함 (읽기 실패는 원래 조회로, 쓰기 실패는 그냥 넘어감)
@Slf4j
public class RedisCacheErrorHandler implements CacheErrorHandler {

  // 캐시에서 데이터를 조회할 때 에러 발생시 호출
  @Override
  public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
    log.warn("캐시 조회에 실패해 원본을 조회합니다: cache={}, key={}", cache.getName(), key, exception);
  }

  // 캐시에 데이터를 저장할 때 에러 발생시 호출
  @Override
  public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
    log.warn("캐시 저장에 실패했습니다: cache={}, key={}", cache.getName(), key, exception);
  }

  // 캐시 데이터를 삭제할 때 에러 발생시 호출
  @Override
  public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
    log.warn("캐시 무효화에 실패했습니다: cache={}, key={}", cache.getName(), key, exception);
  }

  // 캐시 전체를 비울 때 에러 발생시 호출
  @Override
  public void handleCacheClearError(RuntimeException exception, Cache cache) {
    log.warn("캐시 전체 삭제에 실패했습니다: cache={}", cache.getName(), exception);
  }
}