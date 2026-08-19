package com.codeit.mople.domain.watchingsession.listener;

import com.codeit.mople.domain.watchingsession.dto.WatchingSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionEventListener {

  private final SimpMessagingTemplate messagingTemplate;

  //DB 커밋이 성공적으로 완료된 직후에만 실행
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleWatchingSessionEvent(WatchingSessionEvent event) {
    log.info("트랜잭션 커밋 완료, 웹소켓 이벤트 발송 - contentId: {}", event.contentId());

    //웹소켓 브로드캐스팅
    messagingTemplate.convertAndSend("/sub/contents/" + event.contentId().toString() + "/watch", event.changeEvent());
  }
}