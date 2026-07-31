package com.codeit.mople.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.jwt.JwtProvider;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtProvider jwtProvider;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  private String tokenFor(User user) {
    return jwtProvider.createAccessToken(user.getId(), user.getSessionVersion());
  }

  @Test
  @DisplayName("회원가입 성공")
  void signUp_success() throws Exception {
    mockMvc.perform(post("/api/users")
            .param("email", "test@test.com")
            .param("password", "rawPw123")
            .param("name", "testUser"))
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
    mockMvc.perform(post("/api/users")
        .param("email", "invalid-email")
        .param("password", "rawPw123")
        .param("name", "testUser"))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("이메일이 중복되면 409를 반환하고 중복된 이메일 정보를 포함")
  void signUp_returnsConflict_whenEmailDuplicated() throws Exception {
    userRepository.save(User.createUser("dup@test.com", "encoded", "oldUser"));

    mockMvc.perform(post("/api/users")
        .param("email", "dup@test.com")
        .param("password", "rawPw123")
        .param("name", "newUser"))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("USER-002"))
        .andExpect(jsonPath("$.error.details.email").value("dup@test.com"));
  }

  @Test
  @DisplayName("사용자 상세 조회 성공")
  void getUser_success() throws Exception {
    User user = userRepository.save(User.createUser("get@test.com", "encoded", "getUser"));
    String token = tokenFor(user);

    mockMvc.perform(get("/api/users/{userId}", user.getId())
            .header("Authorization", "Bearer " + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("get@test.com"));
  }

  @Test
  @DisplayName("존재하지 않는 사용자를 조회하면 404를 반환")
  void getUser_returnsNotFound_whenUserNotExists() throws Exception {
    User requester = userRepository.save(User.createUser("requester@test.com", "encoded", "requester"));
    String token = tokenFor(requester);

    mockMvc.perform(get("/api/users/{userId}", UUID.randomUUID())
            .header("Authorization", "Bearer " + token))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("USER-001"));
  }

  @Test
  @DisplayName("어드민만 사용자 목록을 조회할 수 있음")
  void getUsers_success_whenAdmin() throws Exception {
    User admin = userRepository.save(User.createUser("admin@test.com", "encoded", "admin"));
    admin.changeRole(Role.ADMIN);
    userRepository.save(admin);
    String adminToken = tokenFor(admin);

    userRepository.save(User.createUser("a@test.com", "encoded", "aa"));
    userRepository.save(User.createUser("b@test.com", "encoded", "bb"));

    mockMvc.perform(get("/api/users")
        .param("limit", "10")
        .param("sortBy", "name")
        .param("sortDirection", "ASCENDING")
        .header("Authorization", "Bearer " + adminToken))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.data").isArray())
        .andExpect(jsonPath("$.data.data.length()").value(3))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  @DisplayName("어드민이 아닌 사용자는 사용자 목록 조회 시 403을 반환함")
  void getUsers_returnsForbidden_whenNotAdmin() throws Exception {
    User normalUser = userRepository.save(User.createUser("nomal@test.com", "encoded", "normalUser"));
    String normalUserToken = tokenFor(normalUser);

    mockMvc.perform(get("/api/users")
        .param("limit", "10")
        .header("Authorization", "Bearer " + normalUserToken))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("이름만 전달하면 이름만 변경")
  void updateProfile_success_nameOnly() throws Exception {
    User user = userRepository.save(User.createUser("update@test.com", "encoded", "oldName"));
    String token = tokenFor(user);

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
        .param("name", "newName")
        .header("Authorization", "Bearer " + token)
        .with(req -> { req.setMethod("PATCH"); return req; })
        .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("newName"));
  }

  @Test
  @DisplayName("이름과 프로필 이미지를 함께 전달하면 둘 다 변경")
  void updateProfile_success_withImage() throws Exception {
    User user = userRepository.save(User.createUser("update2@test.com", "encoded", "oldName"));
    String token = tokenFor(user);
    MockMultipartFile image = new MockMultipartFile("profileImage", "test.jpg", "image/jpeg", "content".getBytes());

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
        .file(image)
        .param("name", "newName")
        .header("Authorization", "Bearer " + token)
        .with(req -> { req.setMethod("PATCH"); return req; })
        .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("newName"))
        .andExpect(jsonPath("$.data.profileImageUrl").isNotEmpty());
  }

  @Test
  @DisplayName("이름이 최대 길이를 초과하면 400을 반환")
  void updateProfile_returnsBadRequest_whenNameTooLong() throws Exception {
    User user = userRepository.save(User.createUser("longname@test.com", "encoded", "oldName"));
    String token = tokenFor(user);
    String tooLongName = "a".repeat(21);

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
        .param("name", tooLongName)
        .header("Authorization", "Bearer " + token)
        .with(req -> { req.setMethod("PATCH"); return req; })
        .with(csrf()))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("본인이 아닌 사용자가 프로필을 수정하면 403을 반환")
  void updateProfile_returnsForbidden_whenNotOwner() throws Exception {
    User owner = userRepository.save(User.createUser("owner@test.com", "encoded", "owner"));
    User attacker = userRepository.save(User.createUser("attacker@test.com", "encoded", "attacker"));
    String attackerToken = tokenFor(attacker);

    mockMvc.perform(multipart("/api/users/{userId}", owner.getId())
        .param("name", "newName")
        .header("Authorization", "Bearer " + attackerToken)
        .with(req -> { req.setMethod("PATCH"); return req; })
        .with(csrf()))
        .andDo(print())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("USER-005"));
  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  void changePassword_success() throws Exception {
    User user = userRepository.save(User.createUser("pw@test.com", passwordEncoder.encode("oldPw123"), "testUser"));
    String token = tokenFor(user);

    mockMvc.perform(patch("/api/users/{userId}/password", user.getId())
        .header("Authorization", "Bearer " + token)
        .param("password", "newPw123")
        .with(csrf()))
        .andDo(print())
        .andExpect(status().isNoContent());

    User updateUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(passwordEncoder.matches("newPw123", updateUser.getPassword())).isTrue();
  }

  @Test
  @DisplayName("본인이 아닌 사용자가 비밀번호를 변경하면 403을 반환")
  void changePassword_returnsForbidden_whenNotOwner() throws Exception {
    User owner = userRepository.save(User.createUser("pw2@test.com", "encoded", "testUser"));
    User attacker = userRepository.save(User.createUser("attacker2@test.com", "encoded", "attacker"));
    String attackerToken = tokenFor(attacker);

    mockMvc.perform(patch("/api/users/{userId}/password", owner.getId())
        .header("Authorization", "Bearer " + attackerToken)
        .param("password", "newPw123")
        .with(csrf()))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CSRF 토큰 없이 프로필 수정 시 403을 반환한다")
  void updateProfile_returnsForbidden_whenNoCsrf() throws Exception {
    User user = userRepository.save(User.createUser("nocsrf@test.com", "encoded", "테스트"));
    String token = tokenFor(user);

    mockMvc.perform(multipart("/api/users/{userId}", user.getId())
            .param("name", "newName")
            .header("Authorization", "Bearer " + token)
            .with(req -> { req.setMethod("PATCH"); return req; }))
        // .with(csrf()) 없음!
        .andDo(print())
        .andExpect(status().isForbidden());  // 403이 나오면 CSRF가 진짜 살아있는 것
  }
}
