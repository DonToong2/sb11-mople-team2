package com.codeit.mople.domain.user.init;

import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
    try {
      userRepository.saveAndFlush(admin);
      log.info("어드민 계정을 생성 성공했습니다.: {}", maskedEmail);
    } catch (DataIntegrityViolationException e) {
      if (isEmailUniqueViolation(e)) {
        log.warn("어드민 계정 동시 초기화가 감지되었습니다. - 다른 인스턴스가 먼저 저장했습니다. : {}", maskedEmail);
        return;
      }
      throw e;
    }
  }

  private boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
    if (!(e.getCause() instanceof ConstraintViolationException cause)) {
      return false;
    }
    String constraintName = cause.getConstraintName();
    return constraintName != null && constraintName.toLowerCase().contains("email");
  }
}
