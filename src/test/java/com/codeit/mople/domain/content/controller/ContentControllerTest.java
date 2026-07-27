package com.codeit.mople.domain.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.service.ContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContentController.class)
@AutoConfigureMockMvc(addFilters = false) //403 에러 방지를 위해 보안 필터 비활성화
@WithMockUser //가짜 인증 사용자 설정
public class ContentControllerTest {

  @Autowired
  public MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ContentService contentService;

  @Test
  @DisplayName("콘텐츠 생성 성공 - 201 Created")
  void createContent_Success() throws Exception {
    UUID adminId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    ContentCreateRequest requestDto = new ContentCreateRequest(
        "MOVIE", "테스트 영화", "설명", List.of("액션"));

    MockMultipartFile requestPart = new MockMultipartFile(
        "request", "", MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8));

    MockMultipartFile thumbnailPart = new MockMultipartFile(
        "thumbnail", "test.png", MediaType.IMAGE_PNG_VALUE,
        "dummy image content".getBytes());

    ContentResponse mockResponse = new ContentResponse(
        contentId, "MOVIE", "테스트 영화", "설명",
        "http://example.com/test.png", List.of("액션"), 0.0,
        0, 0L);

    given(contentService.createContent(any(), any(), any())).willReturn(mockResponse);

    mockMvc.perform(
            multipart(HttpMethod.POST, "/api/contents")
                .file(requestPart)
                .file(thumbnailPart)
                .header("X-User-Id", adminId.toString())
                .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("테스트 영화"))
        .andExpect(jsonPath("$.type").value("MOVIE"))
        .andExpect(jsonPath("$.averageRating").value(0.0));
  }

  @Test
  @DisplayName("콘텐츠 생성 실패 - 필수 값(제목) 누락 시 400 Bad Request")
  void createContent_Fail_Validation() throws Exception {
    UUID adminId = UUID.randomUUID();
    //title을 빈 문자열("")로 설정하여 @NotBlank 검증 실패
    ContentCreateRequest requestDto = new ContentCreateRequest(
        "MOVIE", "", "설명", List.of("액션"));

    MockMultipartFile requestPart = new MockMultipartFile(
        "request", "", MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8));

    MockMultipartFile thumbnailPart = new MockMultipartFile(
        "thumbnail", "test.png", MediaType.IMAGE_PNG_VALUE,
        "dummy image content".getBytes());

    mockMvc.perform(
            multipart(HttpMethod.POST, "/api/contents")
                .file(requestPart)
                .file(thumbnailPart)
                .header("X-User-Id", adminId.toString())
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("콘텐츠 생성 실패 - 필수 헤더(X-User-Id) 누락 시 500 Internal Server Error 반환")
  void createContent_Fail_MissingHeader() throws Exception {
    ContentCreateRequest requestDto = new ContentCreateRequest(
        "MOVIE", "테스트 영화", "설명", List.of("액션"));

    MockMultipartFile requestPart = new MockMultipartFile(
        "request", "", MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8));

    MockMultipartFile thumbnailPart = new MockMultipartFile(
        "thumbnail", "test.png", MediaType.IMAGE_PNG_VALUE,
        "dummy image content".getBytes());

    mockMvc.perform(
            multipart(HttpMethod.POST, "/api/contents")
                .file(requestPart)
                .file(thumbnailPart)
                // .header("X-User-Id", adminId.toString()) 헤더를 의도적으로 누락
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
        .andExpect(status().isInternalServerError());
  }
}
