package com.codeit.mople.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

  @Test
  @DisplayName("User 생성 시 기본값은 USER 권한, 잠금 해제 상태")
  void createUser_defaultValues() {
    User user = User.createUser("test@test.com", "encodedPassword", "testUser");

    assertThat(user.getEmail()).isEqualTo("test@test.com");
    assertThat(user.getRole()).isEqualTo(Role.USER);
    assertThat(user.isLocked()).isFalse();
  }

  @Test
  @DisplayName("프로필 변경 시 이름과 프로필 사진이 갱신됨")
  void updateProfile_success() {
    User user = User.createUser("test@test.com", "encodedPassword", "pastName");

    user.updateProfile("newName", "https://new-image.url");

    assertThat(user.getName()).isEqualTo("newName");
    assertThat(user.getProfileImageUrl()).isEqualTo("https://new-image.url");
  }
}
