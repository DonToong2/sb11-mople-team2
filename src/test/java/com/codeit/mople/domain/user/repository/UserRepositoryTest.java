package com.codeit.mople.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
public class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  void save_and_findByEmail_success() {
    User user = User.createUser("test@test.com", "encoded", "testUser");
    userRepository.save(user);

    var found = userRepository.findByEmail("test@test.com");

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isNotNull();
    assertThat(found.get().getCreatedAt()).isNotNull();
  }

  @Test
  void existsByEmail_returnsTrue_whenEmailExists() {
    userRepository.save(User.createUser("dup@test.com", "encoded", "duplicate"));

    boolean exists = userRepository.existsByEmail("dup@test.com");

    assertThat(exists).isTrue();
  }
}
