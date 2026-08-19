package com.codeit.mople.global.scheduler.directmessage;

import com.codeit.mople.domain.conversation.entity.Conversation;
import com.codeit.mople.domain.conversation.repository.ConversationRepository;
import com.codeit.mople.domain.directmessage.repository.DirectMessageReadRedisRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageReadSyncScheduler {

  private final DirectMessageReadRedisRepository readRedisRepository;
  private final ConversationRepository conversationRepository;

  @Scheduled(fixedDelayString = "${dm.read-sync.delay-ms}")
  @SchedulerLock(
      name = "DirectMessageReadSyncLock",
      lockAtLeastFor = "5s", // 최소 5초간 락 유지
      lockAtMostFor = "9s" // 최대 9초간 락 유지
  )
  @Transactional
  public void syncLastReadAtToDb() {
    Set<Object> dirtyMembers = readRedisRepository.getDirtyMembers();

    if (dirtyMembers == null || dirtyMembers.isEmpty()) {
      return;
    }

    log.info("Redis -> DB 읽음 워터마크 동기화 시작 (총 {}건)", dirtyMembers.size());
    int successCount = 0;

    for (Object memberObj : dirtyMembers) {
      String dirtyMember = (String) memberObj;

      try {
        String[] parts = dirtyMember.split(":");
        UUID conversationId = UUID.fromString(parts[0]);
        UUID userId = UUID.fromString(parts[1]);

        Object cachedValue = readRedisRepository.getLastReadAtForScheduler(dirtyMember);

        if (cachedValue != null) {
          Instant lastReadAt = Instant.parse(cachedValue.toString());

          Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
          if (conversation != null) {
            conversation.updateLastReadAt(userId, lastReadAt);
            successCount++;
            log.debug("읽음 워터마크 동기화 성공 - conversationId: {}, userId: {}", conversationId, userId);
          } else {
            log.warn("읽음 워터마크 동기화 건너뜀 (존재하지 않거나 삭제된 대화방) - conversationId: {}, userId: {}",
                conversationId, userId);
          }
        }
        readRedisRepository.removeDirtyMember(dirtyMember);
      } catch (Exception e) {
        log.error("읽음 워터마크 DB 동기화 중 에러 발생 (Dirty Set 유지) - member: {}", dirtyMember, e);
      }
    }

    log.info("Redis -> DB 읽음 워터마크 동기화 완료 (성공: {}/{}건)", successCount, dirtyMembers.size());
  }
}
