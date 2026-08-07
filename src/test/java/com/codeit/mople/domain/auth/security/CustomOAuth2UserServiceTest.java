package com.codeit.mople.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mople.domain.user.entity.AuthProvider;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class CustomOAuth2UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CustomOAuth2UserService customOAuth2UserService;

  private OAuth2User googleOAuth2User(String email, String name, String picture) {
    return new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("ROLE_USER")),
        Map.of("email", email, "name", name, "picture", picture),
        "email");
  }

  @Test
  @DisplayName("기존 가입 이메일이면 신규 가입 없이 기존 유저를 그대로 사용함")
  void toCustomOAuth2User_returnsExistingUser_whenEmailAlreadyExists() {
    User existingUser = User.createOAuthUser("test@gmail.com", "existingUser", "https://old.image", AuthProvider.GOOGLE);
    UUID existingId = UUID.randomUUID();
    ReflectionTestUtils.setField(existingUser, "id", existingId);
    when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingUser));

    CustomOAuth2User result = customOAuth2UserService.toCustomOAuth2User(
        googleOAuth2User("test@gmail.com", "googleName", "https://new.image"));

    assertThat(result.getUserId()).isEqualTo(existingId);
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("처음 로그인하는 이메일이면 GOOGLE provider로 신규 가입 시킴")
  void toCustomOAuth2User_createNewUser_whenEmailNotFound() {
    when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
      return saved;
    });

    CustomOAuth2User result = customOAuth2UserService.toCustomOAuth2User(
        googleOAuth2User("new@gmail.com", "newUser", "https://new.image"));

    assertThat(result).isNotNull();
    verify(userRepository).save(argThat(saved ->
        saved.getEmail().equals("new@gmail.com")
            && saved.getProvider() == AuthProvider.GOOGLE
            && saved.getPassword() == null));
  }
}
