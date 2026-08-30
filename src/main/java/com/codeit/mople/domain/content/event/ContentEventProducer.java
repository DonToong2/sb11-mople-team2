package com.codeit.mople.domain.content.event;

import com.codeit.mople.global.config.KafkaProperties;
import com.codeit.mople.global.event.KafkaEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
public class ContentEventProducer {

  private final KafkaEventPublisher eventPublisher;

  private final String contentSearchIndexTopic;

  public ContentEventProducer(KafkaEventPublisher eventPublisher, KafkaProperties properties) {
    this.eventPublisher = eventPublisher;
    this.contentSearchIndexTopic = properties.topics().contentSearchIndex();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(ContentSearchIndexEvent event) {
    eventPublisher.publish(
        contentSearchIndexTopic,
        event.contentId().toString(),
        event
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(ContentSearchIndexDeleteEvent event) {
    eventPublisher.publish(
        contentSearchIndexTopic,
        event.contentId().toString(),
        event
    );
  }
}