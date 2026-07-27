package com.codeit.mople.domain.follow.controller;

import com.codeit.mople.domain.follow.controller.api.FollowApi;
import com.codeit.mople.domain.follow.dto.FollowRequest;
import com.codeit.mople.domain.follow.dto.FollowResponse;
import com.codeit.mople.domain.follow.service.FollowService;
import com.codeit.mople.global.security.MoplUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController implements FollowApi {

  private final FollowService followService;

  // MoplUserDetails 임시 작성
  @Override
  public ResponseEntity<FollowResponse> createFollow(MoplUserDetails principal, FollowRequest followRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(followService.follow(followRequest, principal.getUserId()));
  }
}
