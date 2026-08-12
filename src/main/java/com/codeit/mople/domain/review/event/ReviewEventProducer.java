package com.codeit.mople.domain.review.event;

import com.codeit.mople.global.event.KafkaEventPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewEventProducer {

  private final KafkaEventPublisher kafkaEventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publishCreated(UUID contentId) {
    kafkaEventPublisher.publish(
        "review-created",
        new ReviewCreatedEvent(contentId)
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publishUpdated(UUID contentId) {
    kafkaEventPublisher.publish(
        "review-updated",
        new ReviewUpdatedEvent(contentId)
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publishDeleted(UUID contentId) {
    kafkaEventPublisher.publish(
        "review-deleted",
        new ReviewDeletedEvent(contentId)
    );
  }

}
