package com.codeit.mople.global.listener;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.watchingsession.service.WatchingSessionService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final WatchingSessionService watchingSessionService;

  //프론트엔드가 시청자 목록 채널을 구독할 때 자동으로 입장 처리
  @EventListener
  public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = headerAccessor.getDestination();
    String subscriptionId = headerAccessor.getSubscriptionId(); //구독 ID 추출

    if (destination != null && destination.startsWith("/sub/contents") && destination.endsWith("/watch")) {
      try {
        String contentIdStr = destination.split("/")[3];

        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken auth &&
            auth.getPrincipal() instanceof CustomUserDetails user) {

          //웹소켓 세션 속성에 이 구독 ID는 시청자 채널용이다라고 꼬리표(Mark)를 남김
          Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
          if (sessionAttributes != null && subscriptionId != null) {
            sessionAttributes.put(subscriptionId, "WATCH_CHANNEL");
          }

          log.info("유저 입장 userId = {}, contentId = {}", user.getUserId(), contentIdStr);
          watchingSessionService.enterSession(user.getUserId(), UUID.fromString(contentIdStr));
        } else {
          log.warn("입장 실패 : 인증 정보가 없습니다.");
        }
      } catch (Exception e) {
        log.error("입장 처리 중 에러 발생", e);
      }
    }
  }

  //프론트엔드가 브라우저를 닫거나 방을 나갈 대 자동으로 퇴장 처리
  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    if (event.getUser() instanceof UsernamePasswordAuthenticationToken auth &&
        auth.getPrincipal() instanceof CustomUserDetails user) {
      try {
        UUID contentId = watchingSessionService.getWatchingContentId(user.getUserId());
        log.info("유저 퇴장(연결 종료): userId={}, contentId={}", user.getUserId(), contentId);
        watchingSessionService.leaveSession(user.getUserId(), contentId);
      } catch (Exception e) {
        // 시청 중인 방이 없으면 무시
      }
    }
  }
}
