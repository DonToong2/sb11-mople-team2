package com.codeit.mople.domain.user.init;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
    adminInserter.insert(admin, maskEmail(adminProperties.email()));
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
