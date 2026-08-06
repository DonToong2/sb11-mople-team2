package com.codeit.mople.domain.watchingsession.controller;

import com.codeit.mople.domain.watchingsession.dto.ContentChatSendRequest;
import com.codeit.mople.domain.watchingsession.dto.ContentChatDto;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ContentWebSocketController {

  private final SimpMessagingTemplate messagingTemplate;


  //클라이언트가 SEND /pub/contents/{contentId}/chat 으로 메시지를 보내면 이 메서드가 실행
  //WebSocketConfig에서 설정한 ApplicationDestinationPrefixes에 의해 /pub는 생략되어 매핑
  @MessageMapping("/contents/{contentId}/chat")
  public void sendMessage(
      @DestinationVariable("contentId") UUID contentId,
      ContentChatSendRequest request
  ) {
    //응답용 DTO(서버 시간 기록)
    ContentChatDto response = new ContentChatDto(
        contentId.toString(),
        request.senderId(),
        request.senderName(),
        request.message(),
        Instant.now()
    );

    //해당 콘텐츠 방을 구독 중인 모든 유저에게 메시지 브로드캐스팅(SUBSCRIBE /sub/contents/{contentId}/chat)
    messagingTemplate.convertAndSend("/sub/contents/" + contentId.toString() + "/chat", response);
  }
}
