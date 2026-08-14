package com.codeit.mople.domain.directmessage.repository;

import com.codeit.mople.domain.conversation.entity.Conversation;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DirectMessageReadRedisRepository {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String READ_KEY_PREFIX = "dm:read:";
  // DB와 값이 달라져서 동기화가 필요한 대상들을 모아두는 Set
  private static final String DIRTY_SET_KEY = "dm:read:dirty";
  // 7일 동안 조회/수정이 없는 유저의 읽음 키는 레디스에서 자동 삭제
  private static final Duration READ_DATA_TTL = Duration.ofDays(7);

  // 1. 유저가 DM 메시지를 읽었을 때 레디스에 최신 시각을 기록하고 Dirty Set에 등록
  public boolean saveLastReadAt(UUID conversationId, UUID userId, Instant readAt) {
    try {
      String valueKey = READ_KEY_PREFIX + conversationId + ":" + userId;
      String dirtyMember = conversationId + ":" + userId;

      // 레디스에 최신 읽음 시각 문자열 저장 (7일 뒤 자동 만료)
      redisTemplate.opsForValue().set(valueKey, readAt.toString(), READ_DATA_TTL);

      // DB에 갱신할 유저를 Dirty Set에 등록
      redisTemplate.opsForSet().add(DIRTY_SET_KEY, dirtyMember);

      log.info("Redis 읽음 시각 기록 및 대기열 추가 완료 - key: {}, readAt: {}", valueKey, readAt);
      return true;
    } catch (Exception e) {
      log.error("Redis 장애 감지: DB에 직접 읽음 시각 업데이트 (Fallback) 시도 - key: {}", conversationId, e);
      return false;
    }
  }

  // 2. [Cache-Aside] 레디스에서 최신 읽음 시각을 먼저 조회하고, 없으면 DB 값을 반환 후 레디스에 복구
  public Instant getLastReadAt(Conversation conversation, UUID userId) {
    String valueKey = READ_KEY_PREFIX + conversation.getId() + ":" + userId;

    try {
      Object cachedValue = redisTemplate.opsForValue().get(valueKey);

      // 레디스에 최신 값이 있으면 바로 반환
      if (cachedValue != null) {
        log.info("Redis Cache Hit, Redis에서 데이터 로드 - key: {}", valueKey);
        return Instant.parse(cachedValue.toString());
      }

      log.debug("Redis Cache Miss, DB 데이터 로드 시작 - key: {}", valueKey);
      Instant dbValue = conversation.getMyLastReadAt(userId);

      if (dbValue != null) {
        // 레디스에 값 캐싱 복구
        redisTemplate.opsForValue().set(valueKey, dbValue.toString(), READ_DATA_TTL);
      }
      log.info("Redis Cache Miss 처리 완료, DB 값 반환 - key: {}", valueKey);
      return dbValue;
    } catch (Exception e) {
      log.error("Redis 읽음 시각 조회 장애 감지: DB 값으로 Fallback 진행 - key: {}", valueKey);
      return conversation.getMyLastReadAt(userId);
    }
  }

  public Set<Object> getDirtyMembers() {
    return redisTemplate.opsForSet().members(DIRTY_SET_KEY);
  }

  public void removeDirtyMember(String dirtyMember) {
    log.info("Redis Dirty Set 항목 삭제 - member: {}", dirtyMember);
    redisTemplate.opsForSet().remove(DIRTY_SET_KEY, dirtyMember);
  }
}
