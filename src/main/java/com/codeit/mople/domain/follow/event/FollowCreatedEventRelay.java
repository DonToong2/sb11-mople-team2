package com.codeit.mople.domain.follow.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class FollowCreatedEventRelay {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String topic;

  // 생성자, kafka템플릿과 topic이름을 받아서 넣음
  public FollowCreatedEventRelay(KafkaTemplate<String, Object> kafkaTemplate,
      @Value("${kafka.topics.follow-created}")String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  // 1. 기존에 있던 FollowCreatedEvent를 받아서 kafka용으로 만들어 놓은 FollowCreatedEventRelay에 넣음
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void relay(FollowCreatedEvent event) {
    FollowCreatedMessage message = FollowCreatedMessage.from(event);
    String key = message.followeeId().toString();

    // 1. 브로커에 보내놓을 템플릿
    // 2. whenComplate: 브로커에 저장이 완료되면 호출
    // 2.1 result=결과, ex=예외
    kafkaTemplate.send(topic, key, message)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("팔로우 생성 이벤트 발행 실패: followId={}, eventId={}",
                message.followId(), message.eventId(), ex);
            return;
          }
          log.debug("팔로우 생성 이벤트 발행: followId={}, eventId={}",
              message.followId(), message.eventId());
        });
  }

}
