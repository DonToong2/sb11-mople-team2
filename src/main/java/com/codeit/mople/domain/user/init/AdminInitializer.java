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
    if (userRepository.existsByEmailAndRole(adminProperties.email(), Role.ADMIN)) {
      return;
    }
    if (userRepository.existsByEmail(adminProperties.email())) {
      log.warn("Admin initialization skipped: email already taken by non-admin account: {}", adminProperties.email());
      return;
    }
    User admin = User.createAdmin(
        adminProperties.email(),
        passwordEncoder.encode(adminProperties.password()),
        adminProperties.name()
    );
    userRepository.save(admin);
    log.info("Admin account initialized: {}", adminProperties.email());
  }
}
