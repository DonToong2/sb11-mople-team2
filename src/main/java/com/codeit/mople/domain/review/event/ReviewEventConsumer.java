package com.codeit.mople.domain.review.event;

import com.codeit.mople.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventConsumer {

  private final ContentRepository contentRepository;


  // 기본 전파레벨은 REQUIRED(트랜잭션 내에서 이 메서드가 호출되면 트랜잭션 새로 생성하지 않고 그 트랜잭션에 참여)
  @Transactional
  @KafkaListener(topics = "review-created")
  public void handle(ReviewCreatedEvent event) {
    contentRepository.increaseRating(
        event.contentId(),
        event.rating()
    );

    log.info("리뷰 생성 후 콘텐츠 통계 업데이트 완료: contentId={}, rating={}",
        event.contentId(), event.rating());
  }

  @Transactional
  @KafkaListener(topics = "review-updated")
  public void handle(ReviewUpdatedEvent event) {
    contentRepository.updateRating(
        event.contentId(),
        event.oldRating(),
        event.newRating()
    );

    log.info("리뷰 수정 후 콘텐츠 통계 업데이트 완료: contentId={}, oldRating={}, newRating={}",
        event.contentId(), event.oldRating(), event.newRating());
  }

  @Transactional
  @KafkaListener(topics = "review-deleted")
  public void handle(ReviewDeletedEvent event) {
    contentRepository.decreaseRating(
        event.contentId(),
        event.rating()
    );

    log.info("리뷰 삭제 후 콘텐츠 통계 업데이트 완료: contentId={}, rating={}",
        event.contentId(), event.rating());
  }


}
