package com.codeit.mople.domain.user.init;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AdminProperties adminProperties;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
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
      userRepository.save(admin);
      log.info("어드민 계정을 생성 성공했습니다.: {}", maskEmail(adminProperties.email()));
    } catch (DataIntegrityViolationException e) {
      // 동시에 여러 인스턴스가 시작될 때 다른 인스턴스가 먼저 저장한 경우
      log.warn("어드민 계정 동시 초기화가 감지되었습니다.- 다른 인스턴스가 먼저 저장했습니다. : {}", maskEmail(adminProperties.email()));
    }
  }

  private String maskEmail(String email) {
    int atIndex = email.indexOf('@');
    if (atIndex <= 1) {
      return "***" + email.substring(atIndex);
    }
    return email.charAt(0) + "***" + email.substring(atIndex);
  }
}
