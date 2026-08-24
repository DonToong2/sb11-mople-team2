package com.codeit.mople.domain.directmessage.event;

import com.codeit.mople.domain.directmessage.document.DirectMessageDocument;
import com.codeit.mople.domain.directmessage.entity.DirectMessage;
import com.codeit.mople.domain.directmessage.exception.DirectMessageErrorCode;
import com.codeit.mople.domain.directmessage.exception.DirectMessageException;
import com.codeit.mople.domain.directmessage.repository.DirectMessageRepository;
import com.codeit.mople.domain.directmessage.repository.DirectMessageSearchRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageSyncEventListener {

  private final DirectMessageSearchRepository directMessageSearchRepository;
  private final DirectMessageRepository directMessageRepository;

  @Transactional(readOnly = true)
  @KafkaListener(
      topics = "${spring.kafka.topics.direct-message-created:mople.direct-message.created.v1}",
      groupId = "${mople.kafka.consumer.es-sync-group-id}"
  )
  public void handleDirectMessageCreatedForSearch(DirectMessageCreatedEvent event) {
    log.debug("Elasticsearch 검색 동기화 이벤트 수신 - directMessageId: {}", event.directMessageId());

    try {
      DirectMessage message = directMessageRepository.findById(event.directMessageId())
          .orElseThrow(() -> new DirectMessageException(DirectMessageErrorCode.DIRECT_MESSAGE_NOT_FOUND, Map.of("directMessageId", event.directMessageId())));

      DirectMessageDocument document = DirectMessageDocument.from(message);

      directMessageSearchRepository.save(document);
      log.info("Elasticsearch 메시지 저장 완료 - directMessageId: {}", event.directMessageId());
    } catch (Exception e){
      log.error("Elasticsearch 메시지 저장 중 에러 발생 - directMessageId: {}", event.directMessageId(), e);
    }
  }
}
