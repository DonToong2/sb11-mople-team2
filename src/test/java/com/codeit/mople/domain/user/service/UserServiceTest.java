package com.codeit.mople.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mople.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.mople.domain.user.dto.request.UserCreateRequest;
import com.codeit.mople.domain.user.dto.request.UserUpdateRequest;
import com.codeit.mople.domain.user.dto.response.UserDto;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import com.codeit.mople.global.storage.FileStorageService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private FileStorageService fileStorageService;

  @InjectMocks
  private UserService userService;

  private User user;

  @BeforeEach
  void setUp() {
    user = User.createUser("test@test.com", "encodedPassword", "testUser");
  }

  @Test
  @DisplayName("회원가입 성공")
  void signUp_success() {
    UserCreateRequest request = new UserCreateRequest("test@test.com", "rawPw123", "testUser");
    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(passwordEncoder.encode(request.password())).thenReturn("encodedPw");
    when(userRepository.save(any(User.class))).thenReturn(user);

    UserDto response = userService.signUp(request);

    assertThat(response.email()).isEqualTo("test@test.com");
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("이메일 중복 시 예외 발생")
  void signUp_throwsException_whenEmailDuplicated() {
    UserCreateRequest request = new UserCreateRequest("dup@test.com", "rawPw123", "testUser");
    when(userRepository.existsByEmail(request.email())).thenReturn(true);

    assertThatThrownBy(() -> userService.signUp(request))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.DUPLICATE_EMAIL);
  }

  @Test
  @DisplayName("사용자 조회 성공")
  void getUser_success() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    UserDto response = userService.getUser(userId);

    assertThat(response.email()).isEqualTo(user.getEmail());
  }

  @Test
  @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
  void getUser_throwsException_whenUserNotFound() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(userId))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("본인이 아닌 사용자가 프로필을 수정 시 예외 발생")
  void updateProfile_throwsException_whenNotOwner() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    UserUpdateRequest request = new UserUpdateRequest("newName", null);

    assertThatThrownBy(() -> userService.updateProfile(userId, otherUserId, request))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.FORBIDDEN_ACCESS);
  }

  @Test
  @DisplayName("이름만 변경")
  void updateProfile_success_nameOnly() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    UserUpdateRequest request = new UserUpdateRequest("newName", null);

    UserDto response = userService.updateProfile(userId, userId, request);

    assertThat(response.name()).isEqualTo("newName");
  }

  @Test
  @DisplayName("이미지만 변경")
  void updateProfile_success_imageOnly() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(fileStorageService.upload(any())).thenReturn("https://placeholder.mople.com/test.jpg");

    MockMultipartFile image = new MockMultipartFile("profileImage", "test.jpg", "image/jpeg", "content".getBytes());
    UserUpdateRequest request = new UserUpdateRequest(null, image);

    UserDto response = userService.updateProfile(userId, userId, request);

    assertThat(response.profileImageUrl()).isEqualTo("https://placeholder.mople.com/test.jpg");
    assertThat(response.name()).isEqualTo(user.getName());
  }

  @Test
  @DisplayName("이름과 이미지 둘 다 변경")
  void updateProfile_success_both() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(fileStorageService.upload(any())).thenReturn("https://placeholder.mople.com/test.jpg");

    MockMultipartFile image = new MockMultipartFile("profileImage", "test.jpg", "image/jpeg", "content".getBytes());
    UserUpdateRequest request = new UserUpdateRequest("newName", image);

    UserDto response = userService.updateProfile(userId, userId, request);

    assertThat(response.name()).isEqualTo("newName");
    assertThat(response.profileImageUrl()).isEqualTo("https://placeholder.mople.com/test.jpg");
  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  void changePassword_success() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("newPw123")).thenReturn("encodedNewPw");

    ChangePasswordRequest request = new ChangePasswordRequest("newPw123");

    userService.changePassword(userId, userId, request);

    assertThat(user.getPassword()).isEqualTo("encodedNewPw");
  }

  @Test
  @DisplayName("본인이 아닌 사용자가 비밀번호 변경 시 예외 발생")
  void changePassword_throwsException_whenNotOwner() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    ChangePasswordRequest request = new ChangePasswordRequest("newPw123");

    assertThatThrownBy(() -> userService.changePassword(userId, otherUserId, request))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.FORBIDDEN_ACCESS);
  }
}
