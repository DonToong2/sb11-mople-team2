package com.codeit.mople.domain.follow.exception;

import com.codeit.mople.global.error.ConstraintErrorCodes;
import com.codeit.mople.global.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FollowConstraintErrorCodes implements ConstraintErrorCodes {

  @Override
  public Map<String, ErrorCode> get() {
    return Map.of("uk_follows_followee_follower", FollowErrorCode.FOLLOW_DUPLICATE);
  }
}
