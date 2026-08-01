package com.codeit.mople.domain.content.watchingsession.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import com.codeit.mople.domain.content.exception.ContentException;
import com.codeit.mople.domain.content.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mople.domain.content.watchingsession.repository.WatchingSessionQueryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WatchingSessionServiceTest {

  @InjectMocks
  private WatchingSessionService watchingSessionService;

  @Mock
  private WatchingSessionQueryRepository watchingSessionQueryRepository;

  @Test
  @DisplayName("시청 세션 목록 조회 성공 - 빈 목록 반환")
  void getWatchingSessions_Success() {
    UUID contentId = UUID.randomUUID();

    given(watchingSessionQueryRepository.findSessionByCursor(
        any(), any(), any(), any(), anyInt(), any(), any()
    )).willReturn(List.of());

    given(watchingSessionQueryRepository.countSessions(any(), any()))
        .willReturn(0L);

    CursorResponseWatchingSessionDto result = watchingSessionService.getWatchingSessions(
        contentId, null, null, null, 10, "ASCENDING", "createdAt"
    );

    assertNotNull(result);
    assertEquals(0L, result.totalCount());
    assertFalse(result.hasNext());
    assertNull(result.nextCursor());
  }

  @Test
  @DisplayName("시청 세션 목록 조회 실패 - limit이 0 이하일 경우 예외 발생")
  void getWatchingSessions_Fail_LimitTooSmall() {
    UUID contentId = UUID.randomUUID();

    assertThrows(ContentException.class, () -> {
      watchingSessionService.getWatchingSessions(
          contentId, null, null, null, 0, "ASCENDING", "createdAt"
      );
    });
  }
}
