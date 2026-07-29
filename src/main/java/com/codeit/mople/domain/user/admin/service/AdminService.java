package com.codeit.mople.domain.user.admin.service;

import com.codeit.mople.domain.user.dto.response.UserDto;
import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import com.codeit.mople.global.event.UserForceLogoutEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public List<UserDto> getUserList() {
    log.debug("사용자 목록 조회 시작");
    List<UserDto> users = userRepository.findAll(Sort.by("createdAt").ascending())
        .stream()
        .map(UserDto::from)
        .toList();
    log.info("사용자 목록 조회 완료 - 총 {}명", users.size());
    return users;
  }

  @Transactional
  public void changeUserRole(UUID userId, String roleStr) {
    log.debug("권한 변경 시작 - userId: {}, role: {}", userId, roleStr);
    Role role = Role.valueOf(roleStr.toUpperCase());
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    user.changeRole(role);
    eventPublisher.publishEvent(new UserForceLogoutEvent(userId));
    log.info("권한 변경 완료 - userId: {}, role: {}", userId, role);
  }

  @Transactional
  public void changeUserLocked(UUID userId, boolean locked) {
    log.debug("계정 잠금 변경 시작 - userId: {}, locked: {}", userId, locked);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    if (locked) {
      user.lock();
    } else {
      user.unlock();
    }
    eventPublisher.publishEvent(new UserForceLogoutEvent(userId));
    log.info("계정 잠금 변경 완료 - userId: {}, locked: {}", userId, locked);
  }
}
