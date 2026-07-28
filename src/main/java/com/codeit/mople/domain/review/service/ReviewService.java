package com.codeit.mople.domain.review.service;

import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  @Transactional
  public ReviewResponse create() {
    return null;
  }

}
