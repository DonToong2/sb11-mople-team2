package com.codeit.mople.domain.follow.mapper;

import com.codeit.mople.domain.follow.dto.FollowResponse;
import com.codeit.mople.domain.follow.entity.Follow;
import org.springframework.stereotype.Component;

@Component
public class FollowMapper {

  public FollowResponse toFollowResponse(Follow follow) {
    return new FollowResponse(
        follow.getId(),
        follow.getFollowee().getId(),
        follow.getFollower().getId()
    );
  }
}
