package com.codeit.mople.domain.follow.exception;

import com.codeit.mople.global.error.CustomException;

public class FollowException extends CustomException {

  public FollowException(FollowErrorCode errorCode) {
    super(errorCode);
  }

  // 사용 예시 : User user = userRepository.findById(userId)
  //        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
}
