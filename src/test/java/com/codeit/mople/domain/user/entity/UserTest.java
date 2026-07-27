package com.codeit.mople.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  @DisplayName("email이 null이면 User 생성 시 예외가 발생함")
  void createUser_throwsException_whenEmailIsNull() {
    assertThatThrownBy(() -> User.createUser(null, "encodedPassword", "testUser"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("email");
  }

  @Test
  @DisplayName("password가 null이면 User 생성 시 예외가 발생함")
  void createUser_throwsException_whenPasswordIsNull() {
    assertThatThrownBy(() -> User.createUser("test@test.com",  null, "testUser"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("password");
  }

  @Test
  @DisplayName("name이 null이면 User 생성 시 예외가 발생함")
  void createUser_throwsException_whenNameIsNull() {
    assertThatThrownBy(() -> User.createUser("test@test.com", "encodedPassword", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("name");
  }

  @Test
  @DisplayName("프로필 변경 시 이름과 프로필 사진이 갱신됨")
  void updateProfile_success() {
    User user = User.createUser("test@test.com", "encodedPassword", "pastName");

    user.updateProfile("newName", "https://new-image.url");

    assertThat(user.getName()).isEqualTo("newName");
    assertThat(user.getProfileImageUrl()).isEqualTo("https://new-image.url");
  }

  @Test
  @DisplayName("이름만 전달하면 이름만 갱신됨")
  void updateProfile_success_nameOnly() {
    User user = User.createUser("test@test.com", "encodedPassword", "pastName");

    user.updateProfile("newName", null);

    assertThat(user.getName()).isEqualTo("newName");
    assertThat(user.getProfileImageUrl()).isNull();
  }

  @Test
  @DisplayName("이미지만 전달하면 이미지만 갱신됨")
  void updateProfile_success_imageOnly() {
    User user = User.createUser("test@test.com", "encodedPassword", "pastName");

    user.updateProfile(null, "https://new-image.url");

    assertThat(user.getName()).isEqualTo("pastName");
    assertThat(user.getProfileImageUrl()).isEqualTo("https://new-image.url");
  }

  @Test
  @DisplayName("이름과 이미지 모두 null이면 변경되지 않음")
  void updateProfile_noChange_whenBothNull() {
    User user = User.createUser("test@test.com", "encodedPassword", "pastName");

    user.updateProfile(null, null);

    assertThat(user.getName()).isEqualTo("pastName");
    assertThat(user.getProfileImageUrl()).isNull();
  }

  @Test
  @DisplayName("changePassword에 null을 전달하면 예외가 발생함")
  void changePassword_throwsException_whenNewPasswordIsNull() {
    User user = User.createUser("test@test.com",  "encodedPassword", "testUser");

    assertThatThrownBy(() -> user.changePassword(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("encodedNewPassword");
  }

  @Test
  @DisplayName("changeRole에 null을 전달하면 예외가 발생함")
  void changeRole_throwsException_whenRoleIsNull() {
    User user = User.createUser("test@test.com", "encodedPassword", "testUser");

    assertThatThrownBy(() -> user.changeRole(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("role");
  }
}
