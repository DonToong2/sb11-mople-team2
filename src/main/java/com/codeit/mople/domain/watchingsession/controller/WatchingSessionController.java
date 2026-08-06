package com.codeit.mople.domain.watchingsession.controller;

import com.codeit.mople.domain.watchingsession.service.WatchingSessionService;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WatchingSessionController {

  private final WatchingSessionService watchingSessionService;

  //특정 유저가 현재 실시간으로 시청 중인 콘텐츠 ID 단건 조회
  @GetMapping("/users/{watcherId}/watching-sessions")
  public ResponseEntity<?> getWatchingSessionForUser(
      @PathVariable UUID watcherId) {
    UUID contentId = watchingSessionService.getWatchingContentId(watcherId);

    if (contentId == null) {
      return ResponseEntity.noContent().build(); //시청 중인 콘텐츠가 없으면 204 반환
    }

    //JSON 형태로 감싸서 반환
    return ResponseEntity.ok(Map.of("contentId", contentId));
  }

  //특정 콘텐츠를 현재 실시간으로 시청 중인 유저 목록 조회(Redis 기반)
  @GetMapping("/contents/{contentId}/watching-sessions/live")
  public ResponseEntity<?> getLiveWatchingSessions(
      @PathVariable UUID contentId) {
    Set<UUID> watcherIds = watchingSessionService.getWatcherIds(contentId);

    //시청자 수와 유저 ID 목록을 함께 반환
    return ResponseEntity.ok(Map.of(
        "contentId", contentId,
        "watcherCount", watcherIds.size(),
        "watcherIds", watcherIds
    ));
  }
}
