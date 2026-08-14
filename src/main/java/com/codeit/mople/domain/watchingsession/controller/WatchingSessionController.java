package com.codeit.mople.domain.watchingsession.controller;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.watchingsession.controller.api.WatchingSessionApi;
import com.codeit.mople.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mople.domain.watchingsession.service.WatchingSessionService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WatchingSessionController implements WatchingSessionApi {

  private final WatchingSessionService watchingSessionService;

  //특정 유저가 시청 중인 콘텐츠 ID 조회
  @GetMapping("/users/{watcherId}/watching-session")
  public ResponseEntity<Map<String, UUID>> getWatchingSessionForUser(
      @PathVariable UUID watcherId) {
    UUID contentId = watchingSessionService.getWatchingContentId(watcherId);
    return ResponseEntity.ok(Map.of("contentId", contentId));
  }

  //콘텐츠 실시간 시청자 목록 조회
  @GetMapping("/contents/{contentId}/watching-sessions")
  public ResponseEntity<CursorResponseWatchingSessionDto> getWatchingSessions(
      @PathVariable UUID contentId,
      @RequestParam(value = "watcherNameLike", required = false) String watcherNameLike,
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "idAfter", required = false) UUID idAfter,
      @RequestParam(value = "limit", defaultValue = "10") int limit,
      @RequestParam(value = "sortDirection", defaultValue = "ASCENDING") String sortDirection,
      @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    //목록을 조회하러 들어온 순간, 세션 입장 처리를 먼저 실행
    if (userDetails != null) {
      watchingSessionService.enterSession(userDetails.getUserId(), contentId);
    }

    //최신화된 시청자 목록을 조회하여 반환
    CursorResponseWatchingSessionDto response = watchingSessionService.getWatchingSessions(
        contentId, watcherNameLike, cursor, idAfter, limit, sortDirection, sortBy);

    return ResponseEntity.ok(response);
  }
}