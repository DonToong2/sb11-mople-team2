package com.codeit.mople.domain.user.entity;

import com.codeit.mople.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
    @UniqueConstraint(name = "uq_users_provider_provider_id", columnNames = {"provider",
        "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Column(nullable = false)
  private String email;

  @Column
  private String password;

  @Column(nullable = false)
  private String name;

  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private boolean locked;

  @Column
  private String temporaryPassword;

  @Column
  private Instant temporaryPasswordExpiresAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'LOCAL'")
  private AuthProvider provider;

  @Column(name = "provider_id")
  private String providerId;

  private User(String email, String password, String name, Role role, AuthProvider provider) {
    this.email = Objects.requireNonNull(email, "email").toLowerCase(Locale.ROOT);
    this.password = password;
    this.name = Objects.requireNonNull(name, "name");
    this.role = Objects.requireNonNull(role, "role");
    this.provider = Objects.requireNonNull(provider, "provider");
    this.locked = false;
  }

  public static User createUser(String email, String password, String name) {
    Objects.requireNonNull(password, "password");
    return new User(email, password, name, Role.USER, AuthProvider.LOCAL);
  }

  //어드민 계정 자동으로 초기화
  public static User createAdmin(String email, String password, String name) {
    Objects.requireNonNull(password, "password");
    return new User(email, password, name, Role.ADMIN, AuthProvider.LOCAL);
  }

  public static User createOAuthUser(String email, String name, String profileImageUrl,
      AuthProvider provider, String providerId) {
    if (provider == AuthProvider.LOCAL) {
      throw new IllegalArgumentException("OAuth provider must not be LOCAL");
    }
    User user = new User(email, null, name, Role.USER, provider);
    user.profileImageUrl = profileImageUrl;
    user.providerId = Objects.requireNonNull(providerId, "providerId");
    return user;
  }

  // 프로필 변경
  public void updateProfile(String name, String profileImageUrl) {
    if (name != null) {
      this.name = name;
    }
    if (profileImageUrl != null) {
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

  public void issueTemporaryPassword(String encodedTemporaryPassword, Instant expiresAt) {
    this.temporaryPassword = Objects.requireNonNull(encodedTemporaryPassword,
        "encodedTemporaryPassword");
    this.temporaryPasswordExpiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
  }

  public void destroyTemporaryPassword() {
    this.temporaryPassword = null;
    this.temporaryPasswordExpiresAt = null;
  }

  public boolean hasValidTemporaryPassword(Instant now) {
    return temporaryPassword != null && temporaryPasswordExpiresAt != null && now.isBefore(
        temporaryPasswordExpiresAt);
  }
}
