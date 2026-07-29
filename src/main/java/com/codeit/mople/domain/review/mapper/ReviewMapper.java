package com.codeit.mople.domain.review.mapper;

import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import com.codeit.mople.domain.review.entity.Review;
import com.codeit.mople.global.dto.UserSummary;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

  public ReviewResponse toResponse(Review review) {

    return new ReviewResponse(
        review.getId(),
        review.getContent().getId(),
        new UserSummary(
            review.getAuthor().getId(),
            review.getAuthor().getName(),
            review.getAuthor().getProfileImageUrl()
        ),
        review.getText(),
        review.getRating()
    );
  }

}
