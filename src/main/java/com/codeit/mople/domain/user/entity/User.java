package com.codeit.mople.domain.user.entity;

import com.codeit.mople.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String name;

  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private boolean locked;

  private User(String email, String password, String name, Role role) {
    this.email = Objects.requireNonNull(email, "email");
    this.password = Objects.requireNonNull(password, "password");
    this.name = Objects.requireNonNull(name, "name");
    this.role = Objects.requireNonNull(role, "role");
    this.locked = false;
  }

  public static User createUser(String email, String password, String name) {
    return new User(email, password, name, Role.USER);
  }

  // 프로필 변경
  public void updateProfile(String name, String profileImageUrl) {
    if(name != null) {
      this.name = name;
    }
    if(profileImageUrl != null) {
      this.profileImageUrl = profileImageUrl;
    }
  }

  // 비밀번호 변경
  public void changePassword(String encodedNewPassword) {
    this.password = Objects.requireNonNull(encodedNewPassword, "encodedNewPassword");
  }

  // 어드민 기능
  public void changeRole(Role role) {
    this.role = Objects.requireNonNull(role, "role");
  }

  public void lock() {
    this.locked = true;
  }

  public void unlock() {
    this.locked = false;
  }
}
