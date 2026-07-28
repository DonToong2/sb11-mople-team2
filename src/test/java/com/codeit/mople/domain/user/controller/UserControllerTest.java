package com.codeit.mople.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.codeit.mople.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.mople.domain.user.dto.request.UserCreateRequest;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("회원가입 성공")
  void signUp_success() throws Exception {
    UserCreateRequest request = new UserCreateRequest("test@test.com", "rawPw123", "testUser");

    mockMvc.perform(post("/api/users")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value("test@test.com"));

    User savedUser = userRepository.findByEmail("test@test.com")
        .orElseThrow(() -> new AssertionError("가입된 사용자를 찾을 수 없습니다."));

    assertThat(savedUser.getPassword()).isNotEqualTo("rawPw123");
    assertThat(passwordEncoder.matches("rawPw123", savedUser.getPassword())).isTrue();
  }

  @Test
  @DisplayName("이메일 형식이 유효하지 않으면 400을 반환")
  void signUp_returnsBadRequest_whenEmailInvalid() throws Exception {
    UserCreateRequest request = new UserCreateRequest("invalid-email", "rawPw123", "testUser");

    mockMvc.perform(post("/api/users")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("이메일이 중복되면 409를 반환")
  void signUp_returnsConflict_whenEmailDuplicated() throws Exception {
    userRepository.save(User.createUser("dup@test.com", "encoded", "oldUser"));

    UserCreateRequest request = new UserCreateRequest("dup@test.com", "rawPw123", "newUser");

    mockMvc.perform(post("/api/users")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("USER-002"));
  }

  @Test
  @DisplayName("사용자 상세 조회 성공")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void getUser_success() throws Exception {
    User user = userRepository.save(User.createUser("get@test.com", "encoded", "getUser"));

    mockMvc.perform(get("/api/users/{userId}", user.getId()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("get@test.com"));
  }

  @Test
  @DisplayName("존재하지 않는 사용자를 조회하면 404를 반환")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void getUser_returnsNotFound_whenUserNotExists() throws Exception {
    mockMvc.perform(get("/api/users/{userId}", UUID.randomUUID()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("USER-001"));
  }

  @Test
  @DisplayName("이름만 전달하면 이름만 변경")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void updateProfile_success_nameOnly() throws Exception {
    User user = userRepository.save(User.createUser("update@test.com", "encoded", "oldName"));

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
        .param("name", "newName")
        .header("X-User-Id", user.getId().toString())
        .with(req -> { req.setMethod("PATCH"); return req; }))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("newName"));
  }

  @Test
  @DisplayName("이름과 프로필 이미지를 함께 전달하면 둘 다 변경")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void updateProfile_success_withImage() throws Exception {
    User user = userRepository.save(User.createUser("update2@test.com", "encoded", "oldName"));
    MockMultipartFile image = new MockMultipartFile("profileImage", "test.jpg", "image/jpeg", "content".getBytes());

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
        .file(image)
        .param("name", "newName")
        .header("X-User-Id", user.getId().toString())
        .with(req -> { req.setMethod("PATCH"); return req; }))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("newName"))
        .andExpect(jsonPath("$.data.profileImageUrl").isNotEmpty());
  }

  @Test
  @DisplayName("이름이 최대 길이를 초과하면 400을 반환")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void updateProfile_returnsBadRequest_whenNameTooLong() throws Exception {
    User user = userRepository.save(User.createUser("longname@test.com", "encoded", "oldName"));
    String tooLongName = "a".repeat(21);

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
        .param("name", tooLongName)
        .header("X-User-Id", user.getId().toString())
        .with(req -> { req.setMethod("PATCH"); return req; }))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("본인이 아닌 사용자가 프로필을 수정하면 403을 반환")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void updateProfile_returnsForbidden_whenNotOwner() throws Exception {
    User user = userRepository.save(User.createUser("owner@test.com", "encoded", "owner"));
    UUID otherUserId = UUID.randomUUID();

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
        .param("name", "newName")
        .header("X-User-Id", otherUserId.toString())
        .with(req -> { req.setMethod("PATCH"); return req; }))
        .andDo(print())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("USER-005"));
  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void changePassword_success() throws Exception {
    User user = userRepository.save(User.createUser("pw@test.com", passwordEncoder.encode("oldPw123"), "testUser"));
    ChangePasswordRequest request = new ChangePasswordRequest("newPw123");

    mockMvc.perform(patch("/api/users/{userId}/password", user.getId())
        .header("X-User-Id", user.getId().toString())
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("본인이 아닌 사용자가 비밀번호를 변경하면 403을 반환")
  @Disabled("JWT 인증 완성 전까지 임시 비활성화 - SecurityConfig에서 인증 필요 처리됨")
  void changePassword_returnsForbidden_whenNotOwner() throws Exception {
    User user = userRepository.save(User.createUser("pw2@test.com", "encoded", "testUser"));
    UUID otherUserId = UUID.randomUUID();
    ChangePasswordRequest request = new ChangePasswordRequest("newPw123");

    mockMvc.perform(patch("/api/users/{userId}/password", user.getId())
        .header("X-User-Id", otherUserId.toString())
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isForbidden());
  }
}
