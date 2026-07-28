package com.codeit.mople.domain.review.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;

  private final UserRepository userRepository;
  private final ContentRepository contentRepository;

  @Transactional
  public ReviewResponse create(UUID authorId, ReviewCreateRequest request) {

    User author = userRepository.findById(authorId).orElseThrow(() ->
        new CustomException(UserErrorCode.USER_NOT_FOUND));

    Content content = contentRepository.findById(request.contentId()).orElseThrow(() ->
        new CustomException(ContentErrorCode.CONTENT_NOT_FOUND));

    Review review = Review.create(content, author, request.text(), request.rating());

    Review savedReview = reviewRepository.save(review);

    return reviewMapper.toResponse(savedReview);
  }

}
