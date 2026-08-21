package com.codeit.mople.domain.watchingsession.controller;

import com.codeit.mople.domain.watchingsession.dto.ContentChatSendRequest;
import com.codeit.mople.domain.watchingsession.service.WatchingSessionService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ContentWebSocketController {

  private final WatchingSessionService watchingSessionService;

  // 클라이언트가 SEND /pub/contents/{contentId}/chat 으로 메시지를 보내면 실행
  @MessageMapping("/contents/{contentId}/chat")
  public void sendMessage(
      @DestinationVariable("contentId") String contentIdStr,
      @Payload ContentChatSendRequest request,
      Principal principal
  ) {

    watchingSessionService.broadcastChatMessage(contentIdStr, request, principal);
  }
}