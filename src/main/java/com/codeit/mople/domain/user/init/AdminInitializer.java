package com.codeit.mople.domain.user.init;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AdminProperties adminProperties;
  private final AdminInserter adminInserter;

  @Override
  public void run(ApplicationArguments args) {
    log.debug("어드민 계정 초기화 시작");
    if (userRepository.existsByRole(Role.ADMIN)) {
      return;
    }
    if (userRepository.existsByEmail(adminProperties.email())) {
      log.warn("어드민 계정이메일이 이미 사용중입니다. : {}", maskEmail(adminProperties.email()));
      return;
    }
    User admin = User.createAdmin(
        adminProperties.email(),
        passwordEncoder.encode(adminProperties.password()),
        adminProperties.name()
    );
    try {
      adminInserter.insert(admin, maskEmail(adminProperties.email()));
    } catch (DataIntegrityViolationException e) {
      if (isEmailUniqueViolation(e)) {
        log.warn("어드민 계정 동시 초기화가 감지되었습니다. - 다른 인스턴스가 먼저 저장했습니다. : {}",
            maskEmail(adminProperties.email()));
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
    return "uq_users_email".equals(constraintName);
  }

  private String maskEmail(String email) {
    int atIndex = email.indexOf('@');
    if (atIndex < 0) {
      return "***";
    }
    if (atIndex <= 1) {
      return "***" + email.substring(atIndex);
    }
    return email.charAt(0) + "***" + email.substring(atIndex);
  }
}
