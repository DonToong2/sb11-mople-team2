package com.codeit.mople.domain.review.controller;

import com.codeit.mople.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import com.codeit.mople.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping
  public ResponseEntity<ReviewResponse> create(
      @RequestParam UUID authorId, // TODO 김명근: 인증 구현 시 @AuthenticationPrincipal로 대체
      @Valid @RequestBody ReviewCreateRequest request
  ) {

    ReviewResponse response = reviewService.create(authorId, request);

    return ResponseEntity
        .created(URI.create("/api/reviews/" + response.id()))
        .body(response);
  }

}
