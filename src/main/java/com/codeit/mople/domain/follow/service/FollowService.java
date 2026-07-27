package com.codeit.mople.domain.follow.service;

import com.codeit.mople.domain.follow.dto.FollowRequest;
import com.codeit.mople.domain.follow.dto.FollowResponse;
import com.codeit.mople.domain.follow.entity.Follow;
import com.codeit.mople.domain.follow.event.FollowCreatedEvent;
import com.codeit.mople.domain.follow.exception.FollowErrorCode;
import com.codeit.mople.domain.follow.exception.FollowException;
import com.codeit.mople.domain.follow.mapper.FollowMapper;
import com.codeit.mople.domain.follow.repository.FollowRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;
  private final FollowMapper followMapper;
  private final ApplicationEventPublisher publisher;

  @Transactional
  public FollowResponse follow(FollowRequest request, UUID followerId) {

    UUID followeeId = request.followeeId();
    log.debug("팔로우 시도: followeeId={}, followerId={}", followeeId, followerId);

    // 자기 자신 팔로우 안돼
    if (followerId.equals(followeeId)) {
      throw new FollowException(FollowErrorCode.FOLLOW_SELF_NOT_ALLOWED);
    }

    // 팔로우 대상이 존재하는지?
    if (!userRepository.existsById(followeeId)) {
      throw new FollowException(FollowErrorCode.FOLLOWEE_NOT_FOUND);
    }

    // 이미 팔로가 되어있으면 중복 팔로우 안돼
    if (followRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId)) {
      throw new FollowException(FollowErrorCode.FOLLOW_DUPLICATE);
    }

    // 영속화
    User followee = userRepository.getReferenceById(followeeId);
    User follower = userRepository.getReferenceById(followerId);
    Follow saved = followRepository.save(Follow.create(followee, follower));

    log.info("팔로우 성공: followId={}, followeeId={}, followerId={}", saved.getId(), followeeId, followerId);

    // 알림을 위한 이벤트 발행, 알림 도메인 담당과 상의해서 발신자 이름 포함할지 정해야함
    publisher.publishEvent(new FollowCreatedEvent(saved.getId(), followerId, followerId));

    // mapper로 리턴
    return followMapper.toFollowResponse(saved);
  }

}
