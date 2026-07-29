package com.codeit.mople.domain.review.service;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.exception.ContentErrorCode;
import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import com.codeit.mople.domain.review.entity.Review;
import com.codeit.mople.domain.review.mapper.ReviewMapper;
import com.codeit.mople.domain.review.repository.ReviewRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;

  private final UserRepository userRepository;
  private final ContentRepository contentRepository;

  @Transactional
  public ReviewResponse create(UUID authorId, ReviewCreateRequest request) {

    log.debug("리뷰 생성 시도: authorId={}, contentId={}, rating={}",
        authorId, request.contentId(), request.rating());

    User author = userRepository.findById(authorId).orElseThrow(() -> {
      log.warn("리뷰 생성 실패: 사용자를 찾을 수 없습니다. userId={}", authorId);
      return new CustomException(UserErrorCode.USER_NOT_FOUND);
    });

    Content content = contentRepository.findById(request.contentId()).orElseThrow(() -> {
      log.warn("리뷰 생성 실패: 컨텐츠를 찾을 수 없습니다. contentId={}", request.contentId());
      return new CustomException(ContentErrorCode.CONTENT_NOT_FOUND);
    });

    Review review = Review.create(content, author, request.text(), request.rating());

    Review savedReview = reviewRepository.save(review);

    // TODO 김명근: 동시성 문제(Race Condition)는 다음 스프린트 기간 때 개선
    // 컨텐츠의 리뷰 개수, 평점을 조회
    long reviewCount = reviewRepository.countByContentId(content.getId());
    Double averageRating = reviewRepository.findAverageRatingByContentId(content.getId());

    content.updateRatingStats(
        // 컨텐츠가 생성 되었거나 리뷰 삭제 등으로 리뷰가 하나도 없을 경우 0.0점
        averageRating == null ? 0.0 : averageRating,
        (int) reviewCount
    );

    ReviewResponse response = reviewMapper.toResponse(savedReview);
    log.info("리뷰 생성 완료: reviewId={}, userId={}, contentId={}",
        savedReview.getId(), authorId, request.contentId());

    return response;
  }

}
