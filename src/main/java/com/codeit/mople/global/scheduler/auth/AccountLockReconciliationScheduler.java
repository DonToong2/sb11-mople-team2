package com.codeit.mople.global.scheduler.auth;

import com.codeit.mople.domain.auth.repository.AccountLockRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountLockReconciliationScheduler {

  private final UserRepository userRepository;
  private final AccountLockRepository accountLockRepository;

  @Scheduled(cron = "0 0 4 * * *")
  @Transactional(readOnly = true)
  public void reconcileLockedAccounts() {
    List<User> lockedUsers = userRepository.findAllByLockedTrue();

    if(lockedUsers.isEmpty()) {
      return;
    }

    log.info("계정 잠금 상태 재조정 시작 (DB 기준 잠긴 계정 총 {}건)", lockedUsers.size());
    int fixedCount = 0;

    for (User user : lockedUsers) {
      try {
        if(!accountLockRepository.isLocked(user.getId())) {
          accountLockRepository.lock(user.getId());
          fixedCount++;
          log.warn("계정 잠금 불일치 복구 - userId: {} (DB: locked, Redis 미반영 -> 반영 완료)", user.getId());
        }
      } catch (Exception e) {
        log.error("계정 잠금 재조정 중 에러 발생 - userId: {}", user.getId(), e);
      }
    }
    log.info("계정 잠금 상태 재조정 완료 (복구: {}/{}건)", fixedCount, lockedUsers.size());
  }
}
