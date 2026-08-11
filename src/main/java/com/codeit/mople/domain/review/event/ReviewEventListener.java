package com.codeit.mople.domain.review.event;

import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.review.entity.Review;
import com.codeit.mople.domain.review.exception.ReviewErrorCode;
import com.codeit.mople.domain.review.exception.ReviewException;
import com.codeit.mople.domain.review.repository.ReviewRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

  private final ReviewRepository reviewRepository;
  private final ContentRepository contentRepository;

  // TODO 김명근: 추후 Kafka 도입하여 Topic 만들어서 비동기로 처리를 통해 동시성 문제 해결
  // 현재로서는 데이터 정합성을 생각하여 @TransactionEventListener 미사용
  // 기본 전파레벨은 REQUIRED(트랜잭션 내에서 이 메서드가 호출되면 트랜잭션 새로 생성하지 않고 그 트랜잭션에 참여)
  @EventListener
  @Transactional
  public void handle(ReviewCreatedEvent event) {
    Review review = reviewRepository.findById(event.reviewId()).orElseThrow(() ->
        new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("reviewId", event.reviewId()))
    );

    contentRepository.increaseRating(
        event.contentId(),
        review.getRating()
    );

    log.info("리뷰 생성 후 콘텐츠 통계 업데이트 완료: contentId={}, reviewId={}, rating={}",
        event.contentId(), event.reviewId(), review.getRating());
  }

  @EventListener
  @Transactional
  public void handle(ReviewUpdatedEvent event) {
    Review review = reviewRepository.findById(event.reviewId()).orElseThrow(() ->
        new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("reviewId", event.reviewId()))
    );

    contentRepository.updateRating(
        event.contentId(),
        event.oldRating(),
        review.getRating()
    );

    log.info("리뷰 수정 후 콘텐츠 통계 업데이트 완료: contentId={}, reviewId={}, oldRating={}, newRating={}",
        event.contentId(), event.reviewId(), event.oldRating(), review.getRating());
  }

  @EventListener
  @Transactional
  public void handle(ReviewDeletedEvent event) {
    contentRepository.decreaseRating(
        event.contentId(),
        event.rating()
    );

    log.info("리뷰 삭제 후 콘텐츠 통계 업데이트 완료: contentId={}, rating={}",
        event.contentId(), event.rating());
  }


}
