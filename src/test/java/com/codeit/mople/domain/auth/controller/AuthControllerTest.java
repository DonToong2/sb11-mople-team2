package com.codeit.mople.domain.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.codeit.mople.domain.auth.dto.request.SignInRequest;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {
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
  @DisplayName("로그인 성공 시 토큰을 발급")
  void signIn_success() throws Exception {
    userRepository.save(User.createUser("test@test.com", passwordEncoder.encode("rawPw123"), "testUser"));
    SignInRequest request = new SignInRequest("test@test.com", "rawPw123");

    mockMvc.perform(post("/api/auth/sign-in")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 로그인하면 401을 반환")
  void signIn_returnsUnauthorized_whenEmailNotFound() throws Exception {
    SignInRequest request = new SignInRequest("nobody@test.com", "rawPw123");

    mockMvc.perform(post("/api/auth/sign-in")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("AUTH-001"));
  }

  @Test
  @DisplayName("비밀번호가 틀리면 401을 반환")
  void signIn_returnsUnauthorized_whenPasswordWrong() throws Exception {
    userRepository.save(User.createUser("test2@test.com", passwordEncoder.encode("correctPw"), "testUser"));
    SignInRequest request = new SignInRequest("test2@test.com", "wrongPw");

    mockMvc.perform(post("/api/auth/sign-in")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("AUTH-001"));
  }

  @Test
  @DisplayName("재로그인 시 이전 토큰으로는 인증이 필요한 API에 접근할 수 없음")
  void reSignIn_invalidatesOldToken() throws Exception {
    User user = userRepository.save(User.createUser("multi@test.com", passwordEncoder.encode("rawPw123"), "testUser"));
    SignInRequest request = new SignInRequest("multi@test.com", "rawPw123");

    String firstResponse = mockMvc.perform(post("/api/auth/sign-in")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andReturn().getResponse().getContentAsString();

    String oldToken = objectMapper.readTree(firstResponse).get("data").get("accessToken").asText();

    mockMvc.perform(post("/api/auth/sign-in")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/users/{userId}", user.getId())
        .header("Authorization", "Bearer " + oldToken))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("로그아웃 요청은 204를 반환")
  void signOut_success() throws Exception {
    mockMvc.perform(post("/api/auth/sign-out"))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("CSRF 토큰 발급 요청은 200을 반환하고 쿠키를 내려줌")
  void csrfToken_success() throws Exception {
    mockMvc.perform(get("/api/auth/csrf-token"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }
}
