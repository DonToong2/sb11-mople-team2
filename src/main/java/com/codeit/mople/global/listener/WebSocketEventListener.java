package com.codeit.mople.global.listener;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.content.exception.ContentException;
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

  //프론트엔드가 방을 나갈 때 자동으로 퇴장 처리
  @EventListener
  public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String subscriptionId = headerAccessor.getSubscriptionId();

    if (subscriptionId != null && headerAccessor.getSessionAttributes() != null) {
      String contentIdStr = (String) headerAccessor.getSessionAttributes().get(subscriptionId);

      if (contentIdStr != null && headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken auth &&
          auth.getPrincipal() instanceof CustomUserDetails user) {
        try {
          log.info("유저 퇴장(구독 해제): userId={}, contentId={}", user.getUserId(), contentIdStr);
          watchingSessionService.leaveSession(user.getUserId(), UUID.fromString(contentIdStr));
        } catch (Exception e) {
          log.error("구독 해제(퇴장) 처리 중 에러 발생 - userId: {}, contentId: {}", user.getUserId(), contentIdStr, e);
        } finally {
          // 메모리 누수를 방지하기 위해 세션 속성에서 해당 구독 ID 제거
          headerAccessor.getSessionAttributes().remove(subscriptionId);
        }
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
      } catch (ContentException e) {
        //시청 중인 방이 없어서 발생하는 정상적인 예외이므로 무시
        log.debug("연결 종료 시 퇴장 처리 무시(시청 중인 방 없음) - userId: {}", user.getUserId());
      } catch (Exception e) {
        //Redis 장애 등 예상치 못한 시스템 에러는 구조화하여 로그로 남김
        log.error("연결 종료(퇴장) 처리 중 시스템 에러 발생 - userId: {}", user.getUserId(), e);
      }
    }
  }
}
