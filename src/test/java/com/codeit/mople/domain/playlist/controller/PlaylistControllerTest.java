package com.codeit.mople.domain.playlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistOwnerResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.service.PlaylistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlaylistController.class)
public class PlaylistControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private PlaylistService playlistService;

  private UUID ownerId;
  private PlaylistCreateRequest request;
  private String title;
  private String description;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    title = "새 플레이리스트 (1)";
    description = "새로운 플레이리스트입니다.";
    request = new PlaylistCreateRequest(title, description);
  }

  @Test
  @DisplayName("플레이리스트 생성 성공")
  void create_success() throws Exception {
    // given

    // setUp()에서 ownerId, request, title, description 초기화

    UUID playlistId = UUID.randomUUID();

    PlaylistOwnerResponse ownerResponse = new PlaylistOwnerResponse(
        ownerId,
        "사용자",
        "profileImageUrl"
    );

    PlaylistResponse response = new PlaylistResponse(
        playlistId,
        ownerResponse,
        title,
        description,
        Instant.now(),
        0L,
        true,
        List.of()
    );

    given(playlistService.create(ownerId, request))
        .willReturn(response);

    // when & then
    // 결과 중심(상태 검증)
    mockMvc.perform(post("/api/playlists")
            .with(user("사용자")) // 인증(미호출 시 401 에러)
            .with(csrf()) // 인가(미호출 시 403 에러)
            .param("ownerId", ownerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(playlistId.toString()))
        .andExpect(jsonPath("$.owner.userId").value(ownerId.toString()))
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.description").value(description))
        .andExpect(jsonPath("$.subscriberCount").value(0L))
        .andExpect(jsonPath("$.contents").isArray());

    // 행위 중심(PlaylistService.create() 메서드가 호출되었는지 검증)
    verify(playlistService).create(eq(ownerId), any(PlaylistCreateRequest.class));
  }

  @Test
  @DisplayName("플레이리스트 생성 실패 - 제목이 비어있음")
  void create_fail_BlankTitle() throws Exception {
    // given
    PlaylistCreateRequest invalidRequest = new PlaylistCreateRequest("", description);

    // when & then
    mockMvc.perform(post("/api/playlists")
            .with(user("사용자"))
            .with(csrf())
            .param("ownerId", ownerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());

    // Valid에서 400 에러가 발생하기 때문에 PlaylistService.create() 메서드는 호출되지 않아야함
    verifyNoInteractions(playlistService);
  }

  @Test
  @DisplayName("플레이리스트 생성 실패 - 설명이 비어있음")
  void create_fail_BlankDescription() throws Exception {
    // given
    PlaylistCreateRequest invalidRequest = new PlaylistCreateRequest(title, "");

    // when & then
    mockMvc.perform(post("/api/playlists")
            .with(user("사용자"))
            .with(csrf())
            .param("ownerId", ownerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());

    verifyNoInteractions(playlistService);
  }

}
