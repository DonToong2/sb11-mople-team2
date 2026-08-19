package com.codeit.mople.domain.auth.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.codeit.mople.domain.auth.repository.AccountLockRepository;
import com.codeit.mople.domain.auth.repository.RefreshTokenRepository;
import com.codeit.mople.domain.auth.repository.SessionTokenRepository;
import com.codeit.mople.global.event.ForceLogoutReason;
import com.codeit.mople.global.event.UserAccountStatusChangedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountLockRedisSyncListenerTest {

  @InjectMocks
  private AccountLockRedisSyncListener listener;

  @Mock
  private AccountLockRepository accountLockRepository;

  @Mock
  private SessionTokenRepository sessionTokenRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  private final UUID userId = UUID.randomUUID();

  @Test
  @DisplayName("ACCOUNT_LOCKED 이벤트일 경우 세션/리프레시 토큰을 무효화하고 Redis 잠금을 설정함")
  void handle_syncsAllRedisState_whenAccountLocked() {
    // when
    listener.handle(new UserAccountStatusChangedEvent(userId, ForceLogoutReason.ACCOUNT_LOCKED, true));

    // then
    verify(sessionTokenRepository).invalidate(userId);
    verify(refreshTokenRepository).invalidate(userId);
    verify(accountLockRepository).lock(userId);
  }

  @Test
  @DisplayName("ACCOUNT_UNLOCKED 이벤트일 경우 Redis 잠금만 해제하고 세션/리프레시는 건드리지 않음")
  void handle_onlyUnlocksRedis_whenAccountUnlocked() {
    // when
    listener.handle(new UserAccountStatusChangedEvent(userId, ForceLogoutReason.ACCOUNT_UNLOCKED, false));

    // then
    verify(accountLockRepository).unlock(userId);
    verify(sessionTokenRepository, never()).invalidate(any());
    verify(refreshTokenRepository, never()).invalidate(any());
  }

  @Test
  @DisplayName("ROLE_CHANGE 등 잠금과 무관한 이벤트는 무시함")
  void handle_ignoresUnrelatedReason() {
    // when
    listener.handle(new UserAccountStatusChangedEvent(userId, ForceLogoutReason.ROLE_CHANGE, true));

    // then
    verifyNoMoreInteractions(accountLockRepository, sessionTokenRepository, refreshTokenRepository);
  }

  @Test
  @DisplayName("첫 시도가 실패하면 재시도해서 결국 성공함")
  void handle_retriesAndSucceeds_whenFirstAttemptFails() {
    // given
    willThrow(new RuntimeException("redis down"))
        .willDoNothing()
        .given(accountLockRepository).lock(userId);

    // when
    listener.handle(new UserAccountStatusChangedEvent(userId, ForceLogoutReason.ACCOUNT_LOCKED, true));

    // then
    verify(accountLockRepository, timeout(2000).times(2)).lock(userId);
  }
}
