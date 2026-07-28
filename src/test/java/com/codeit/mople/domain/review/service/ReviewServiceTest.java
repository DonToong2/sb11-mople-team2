package com.codeit.mople.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.mople.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import com.codeit.mople.domain.review.entity.Review;
import com.codeit.mople.domain.review.mapper.ReviewMapper;
import com.codeit.mople.domain.review.repository.ReviewRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
  private ReviewCreateRequest request;

  @BeforeEach
  void setUp() {
    authorId = UUID.randomUUID();
    contentId = UUID.randomUUID();
    author = mock(User.class);
    content = mock(Content.class);

    reviewText = "리뷰 내용";
    reviewRating = 5.0;
    request = new ReviewCreateRequest(contentId, reviewText, reviewRating);
  }

  @Test
  @DisplayName("리뷰 생성 성공")
  void create_success() {
    // given

    // BeforeEach에서 author, authorId, content, contentId, Review Create Request 초기화

    Review review = Review.create(content, author, request.text(), request.rating());

    ReviewResponse response = mock(ReviewResponse.class);

    // User 조회 → Content 조회 → Review 저장 → ReviewMapper 호출 순
    given(userRepository.findById(authorId))
        .willReturn(Optional.of(author));

    given(contentRepository.findById(contentId))
        .willReturn(Optional.of(content));

    given(reviewRepository.save(any(Review.class)))
        .willReturn(review);

    given(reviewMapper.toResponse(review))
        .willReturn(response);

    // when
    ReviewResponse result = reviewService.create(authorId, request);

    // then
    assertThat(result).isEqualTo(response);

    verify(userRepository).findById(authorId);
    verify(contentRepository).findById(contentId);
    verify(reviewRepository).save(any(Review.class));
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
        reviewService.create(authorId, request))
        .isInstanceOf(CustomException.class);

    verify(userRepository).findById(authorId);
    verifyNoInteractions(contentRepository, reviewRepository, reviewMapper);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 컨텐츠가 존재하지 않음")
  void create_fail_notFoundContent() {
    // given

    // BeforeEach에서 authorId 초기화

    given(userRepository.findById(authorId))
        .willReturn(Optional.of(author));

    given(contentRepository.findById(contentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() ->
        reviewService.create(authorId, request))
        .isInstanceOf(CustomException.class);

    verify(userRepository).findById(authorId);
    verify(contentRepository).findById(contentId);
    verifyNoInteractions(reviewRepository, reviewMapper);
  }

}
