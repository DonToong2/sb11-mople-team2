package com.codeit.mople.domain.user.init;

import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class AdminInserter {

  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void insert(User admin, String maskedEmail) {
    userRepository.saveAndFlush(admin);
    log.info("어드민 계정을 생성 성공했습니다.: {}", maskedEmail);
  }
}
