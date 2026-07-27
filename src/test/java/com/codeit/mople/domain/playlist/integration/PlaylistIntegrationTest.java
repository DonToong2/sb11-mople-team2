package com.codeit.mople.domain.playlist.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.entity.Playlist;
import com.codeit.mople.domain.playlist.repository.PlaylistRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class PlaylistIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlaylistRepository playlistRepository;

  private User savedOwner;
  private PlaylistCreateRequest request;
  private String title;
  private String description;

  @BeforeEach
  void setUp() {
    savedOwner = userRepository.save(
        User.createUser("test@test.com", "12345678", "test")
    );
    title = "새 플레이리스트 (1)";
    description = "새로운 플레이리스트입니다.";
    request = new PlaylistCreateRequest(title, description);
  }

  @Test
  @DisplayName("플레이리스트 생성 성공")
  void create_success() throws Exception {
    // given

    // BeforeEach에서 DB에 저장된 사용자, PlaylistCreateRequest 초기화

    // when & then
    // 상태 검증
    MvcResult result = mockMvc.perform(post("/api/playlists")
            .with(user("사용자"))
            .with(csrf())
            .param("ownerId", savedOwner.getId().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.owner.userId").value(savedOwner.getId().toString()))
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.description").value(description))
        .andExpect(jsonPath("$.subscriberCount").value(0L))
        .andExpect(jsonPath("$.contents").isArray())
        .andReturn();

    // DB 검증
    // 응답 추출
    PlaylistResponse response = objectMapper.readValue(
        result.getResponse().getContentAsString(), PlaylistResponse.class
    );

    Playlist playlist = playlistRepository.findById(response.id()).orElseThrow();

    assertThat(playlist.getOwner()).isEqualTo(savedOwner);
    assertThat(playlist.getTitle()).isEqualTo(title);
    assertThat(playlist.getDescription()).isEqualTo(description);
  }

  @Test
  @DisplayName("플레이리스트 생성 실패 - 제목이 비어있음(400 에러)")
  void create_fail_blankTitle() throws Exception {
    // given
    PlaylistCreateRequest invalidRequest = new PlaylistCreateRequest("", description);

    // when & then
    mockMvc.perform(post("/api/playlists")
            .with(user("사용자"))
            .with(csrf())
            .param("ownerId", savedOwner.getId().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    assertThat(playlistRepository.count()).isZero();
  }

  @Test
  @DisplayName("플레이리스트 생성 실패 - 설명이 비어있음(400 에러)")
  void create_fail_blankDescription() throws Exception {
    // given
    PlaylistCreateRequest invalidRequest = new PlaylistCreateRequest(title, "");

    // when & then
    mockMvc.perform(post("/api/playlists")
            .with(user("사용자"))
            .with(csrf())
            .param("ownerId", savedOwner.getId().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    assertThat(playlistRepository.count()).isZero();
  }

  @Test
  @DisplayName("플레이리스트 생성 실패 - 인증되지 않은 사용자")
  void create_fail_unauthorized() throws Exception {
    // when & then
    mockMvc.perform(post("/api/playlists")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("플레이리스트 생성 실패 - 사용자가 존재하지 않음(404 에러)")
  void create_fail_notFoundUser() throws Exception {
    // given
    UUID notExistOwnerId = UUID.randomUUID();

    // BeforeEach에서 PlaylistCreateRequest 초기화

    // when & then
    mockMvc.perform(post("/api/playlists")
            .with(user("사용자"))
            .with(csrf())
            .param("ownerId", notExistOwnerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    // 플레이리스트가 저장되지 말아야 함
    assertThat(playlistRepository.count()).isZero();
  }

}
