package com.codeit.mople.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.mople.domain.review.dto.request.ReviewUpdateRequest;
import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import com.codeit.mople.domain.review.entity.Review;
import com.codeit.mople.domain.review.exception.ReviewErrorCode;
import com.codeit.mople.domain.review.exception.ReviewException;
import com.codeit.mople.domain.review.mapper.ReviewMapper;
import com.codeit.mople.domain.review.repository.ReviewRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewMapper reviewMapper;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ContentRepository contentRepository;

  @InjectMocks
  private ReviewService reviewService;

  private UUID authorId;
  private UUID contentId;
  private User author;
  private Content content;
  private String reviewText;
  private Double reviewRating;
  private ReviewCreateRequest createRequest;

  private UUID reviewId;
  private Review review;
  private ReviewUpdateRequest updateRequest;

  @BeforeEach
  void setUp() {
    authorId = UUID.randomUUID();
    contentId = UUID.randomUUID();
    author = mock(User.class);
    content = mock(Content.class);

    reviewText = "리뷰 내용";
    reviewRating = 5.0;
    createRequest = new ReviewCreateRequest(contentId, reviewText, reviewRating);

    reviewId = UUID.randomUUID();
    review = Review.create(content, author, reviewText, reviewRating);
    updateRequest = new ReviewUpdateRequest("수정된 내용", 3.0);
  }

  @Nested
  @DisplayName("리뷰 생성")
  class Create {

    @Test
    @DisplayName("리뷰 생성 성공")
    void create_success() {
      // given

      // BeforeEach에서 author, authorId, content, contentId, Review Create Request 초기화

      Review review = Review.create(content, author, createRequest.text(), createRequest.rating());

      ReviewResponse response = mock(ReviewResponse.class);

      // User 조회 → Content 조회 → Review 저장 → ReviewMapper 호출 순
      given(userRepository.findById(authorId))
          .willReturn(Optional.of(author));

      given(contentRepository.findById(contentId))
          .willReturn(Optional.of(content));

      // content는 mock 객체이고 서비스 코드에서 content.getId()를 사용하기 때문에 Id를 Stub 해줘야 함
      given(content.getId()).willReturn(contentId);

      given(reviewRepository.save(any(Review.class)))
          .willReturn(review);

      given(reviewRepository.countByContentId(contentId))
          .willReturn(1L);

      given(reviewRepository.findAverageRatingByContentId(contentId))
          .willReturn(createRequest.rating());

      given(reviewMapper.toResponse(review))
          .willReturn(response);

      // when
      ReviewResponse result = reviewService.create(authorId, createRequest);

      // then
      assertThat(result).isEqualTo(response);

      verify(userRepository).findById(authorId);
      verify(contentRepository).findById(contentId);
      verify(reviewRepository).save(any(Review.class));
      verify(reviewRepository).countByContentId(contentId);
      verify(reviewRepository).findAverageRatingByContentId(contentId);

      verify(content).updateRatingStats(createRequest.rating(), 1);

      verify(reviewMapper).toResponse(review);
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 사용자가 존재하지 않음")
    void create_fail_notFoundUser() {
      // given

      // BeforeEach에서 authorId 초기화

      given(userRepository.findById(authorId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          reviewService.create(authorId, createRequest))
          .isInstanceOf(CustomException.class);

      verify(userRepository).findById(authorId);
      verifyNoInteractions(contentRepository, reviewRepository, reviewMapper);
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 콘텐츠가 존재하지 않음")
    void create_fail_notFoundContent() {
      // given

      // BeforeEach에서 authorId 초기화

      given(userRepository.findById(authorId))
          .willReturn(Optional.of(author));

      given(contentRepository.findById(contentId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          reviewService.create(authorId, createRequest))
          .isInstanceOf(CustomException.class);

      verify(userRepository).findById(authorId);
      verify(contentRepository).findById(contentId);
      verifyNoInteractions(reviewRepository, reviewMapper);
    }

  }

  @Nested
  @DisplayName("리뷰 수정")
  class Update {

    @Test
    @DisplayName("리뷰 수정 성공")
    void update_success() {
      // given

      // BeforeEach에서 reviewId, review, authorId, contentId, updateRequest 초기화

      ReviewResponse response = mock(ReviewResponse.class);

      given(reviewRepository.findById(reviewId))
          .willReturn(Optional.of(review));

      // 리뷰 작성자 설정
      given(author.getId())
          .willReturn(authorId);

      // 콘텐츠 ID 설정
      given(content.getId())
          .willReturn(contentId);

      given(reviewRepository.countByContentId(contentId))
          .willReturn(1L);

      given(reviewRepository.findAverageRatingByContentId(contentId))
          .willReturn(updateRequest.rating());

      given(reviewMapper.toResponse(review))
          .willReturn(response);

      // when
      ReviewResponse result = reviewService.update(reviewId, updateRequest, authorId);

      // then
      assertThat(result).isEqualTo(response);
      assertThat(review.getText()).isEqualTo(updateRequest.text());
      assertThat(review.getRating()).isEqualTo(updateRequest.rating());

      verify(reviewRepository).findById(reviewId);
      verify(reviewRepository).countByContentId(contentId);
      verify(reviewRepository).findAverageRatingByContentId(contentId);
      verify(reviewMapper).toResponse(review);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 리뷰가 존재하지 않음")
    void update_fail_notFoundReview() {
      // given

      // BeforeEach에서 reviewId, authorId, updateRequest 초기화

      given(reviewRepository.findById(reviewId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          reviewService.update(reviewId, updateRequest, authorId)
      )
          .isInstanceOf(ReviewException.class)
          .extracting("errorCode")
          .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);

      verify(reviewRepository).findById(reviewId);

      verifyNoInteractions(author, content, reviewMapper);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 리뷰 작성자가 아님")
    void update_fail_forbidden() {
      // given
      UUID noAuthorId = UUID.randomUUID();

      // BeforeEach에서 reviewId, authorId, updateRequest를 초기화

      given(reviewRepository.findById(reviewId))
          .willReturn(Optional.of(review));

      given(author.getId())
          .willReturn(authorId);

      // when & then
      assertThatThrownBy(() ->
          reviewService.update(reviewId, updateRequest, noAuthorId)
      )
          .isInstanceOf(ReviewException.class)
          .extracting("errorCode")
          .isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN);
    }

  }

  @Nested
  @DisplayName("리뷰 삭제")
  class Delete {

    @Test
    @DisplayName("리뷰 삭제 성공")
    void delete_success() {
      // given
      
      // BeforeEach에서 review, reviewId, authorId 초기화
      
      given(reviewRepository.findById(reviewId))
          .willReturn(Optional.of(review));
      
      given(author.getId())
          .willReturn(authorId);
      
      given(content.getId())
          .willReturn(contentId);
      
      // reviewRepository.delete() 메서드는 void이기 때문에 값을 반환하지 않음

      // 콘텐츠 개수는 0개여야 하고
      given(reviewRepository.countByContentId(contentId))
          .willReturn(0L);

      // 콘텐츠 개수가 0개이기 때문에 0.0점을 반환
      given(reviewRepository.findAverageRatingByContentId(contentId))
          .willReturn(0.0);
      
      // when
      reviewService.delete(reviewId, authorId);
      
      // then
      verify(reviewRepository).findById(reviewId);
      verify(reviewRepository).delete(review);
      verify(content).updateRatingStats(0.0, 0);
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 리뷰가 존재하지 않음")
    void delete_fail_notFoundReview() {
      // given
      given(reviewRepository.findById(reviewId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          reviewService.delete(reviewId, authorId)
      )
          .isInstanceOf(ReviewException.class)
          .extracting("errorCode")
          .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);

      verify(reviewRepository).findById(reviewId);

      verify(reviewRepository, never()).delete(any(Review.class));
      verifyNoInteractions(content);
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 리뷰 작성자가 아님")
    void delete_fail_forbidden() {
      // given
      UUID noAuthorId = UUID.randomUUID();

      given(reviewRepository.findById(reviewId))
          .willReturn(Optional.of(review));

      given(author.getId())
          .willReturn(authorId);

      // when & then
      assertThatThrownBy(() ->
          reviewService.delete(reviewId, noAuthorId)
      )
          .isInstanceOf(ReviewException.class)
          .extracting("errorCode")
          .isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN);

      verify(reviewRepository).findById(reviewId);
      verify(author).getId();

      verify(reviewRepository, never()).delete(any(Review.class));
      verifyNoInteractions(content);
    }
  }

}
