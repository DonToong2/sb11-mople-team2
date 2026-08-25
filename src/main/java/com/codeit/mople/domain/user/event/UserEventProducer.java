package com.codeit.mople.domain.user.event;

import com.codeit.mople.global.config.KafkaProperties;
import com.codeit.mople.global.event.KafkaEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class UserEventProducer {

  private final KafkaEventPublisher eventPublisher;

  private final String userSearchIndexTopic;

  public UserEventProducer(KafkaEventPublisher eventPublisher, KafkaProperties properties) {
    this.eventPublisher = eventPublisher;
    this.userSearchIndexTopic = properties.topics().userSearchIndex();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(UserSearchIndexEvent event) {
    eventPublisher.publish(
        userSearchIndexTopic,
        event.userId().toString(),
        event
    );
  }

}