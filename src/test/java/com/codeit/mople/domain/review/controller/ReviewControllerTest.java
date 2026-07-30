package com.codeit.mople.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import com.codeit.mople.domain.review.service.ReviewService;
import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.config.SecurityConfig;
import com.codeit.mople.global.dto.UserSummary;
import com.codeit.mople.global.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(SecurityConfig.class)
@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private JwtProvider jwtProvider;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private ReviewService reviewService;

  private CustomUserDetails userDetails;
  private UUID authorId;
  private UUID contentId;
  private UUID reviewId;
  private String reviewText;
  private Double reviewRating;
  private ReviewCreateRequest request;

  @BeforeEach
  void setUp() {
    authorId = UUID.randomUUID();
    userDetails = new CustomUserDetails(authorId, Role.USER);

    contentId = UUID.randomUUID();
    reviewId = UUID.randomUUID();

    reviewText = "리뷰 내용";
    reviewRating = 5.0;

    request = new ReviewCreateRequest(contentId, reviewText, reviewRating);
  }

  @Test
  @DisplayName("리뷰 생성 성공")
  void create_success() throws Exception {
    // given

    // BeforeEach에서 authorId, contentId, reviewId, Review Create Request 초기화

    ReviewResponse response = new ReviewResponse(
        reviewId,
        contentId,
        new UserSummary(
            authorId,
            "test",
            null
        ),
        reviewText,
        reviewRating
    );

    given(reviewService.create(eq(authorId), any(ReviewCreateRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user(userDetails))
            .with(csrf())
            .param("authorId", authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/reviews/" + reviewId.toString()))
        .andExpect(jsonPath("$.id").value(reviewId.toString()))
        .andExpect(jsonPath("$.contentId").value(contentId.toString()))
        .andExpect(jsonPath("$.author.userId").value(authorId.toString()))
        .andExpect(jsonPath("$.author.name").value("test"))
        .andExpect(jsonPath("$.author.profileImageUrl").isEmpty())
        .andExpect(jsonPath("$.text").value(reviewText))
        .andExpect(jsonPath("$.rating").value(reviewRating)
        );

    verify(reviewService).create(eq(authorId), any(ReviewCreateRequest.class));
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 콘텐츠ID가 없음(400 에러)")
  void create_fail_nullContentId() throws Exception {
    // given
    ReviewCreateRequest invalidRequest = new ReviewCreateRequest(null, reviewText, reviewRating);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user(userDetails))
            .with(csrf())
            .param("authorId", authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 리뷰 내용이 비어있음(400 에러)")
  void create_fail_blankText() throws Exception {
    // given
    ReviewCreateRequest invalidRequest = new ReviewCreateRequest(contentId, "", reviewRating);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user(userDetails))
            .with(csrf())
            .param("authorId", authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 별점이 없음(400 에러)")
  void create_fail_nullRating() throws Exception {
    // given
    ReviewCreateRequest invalidRequest = new ReviewCreateRequest(contentId, reviewText, null);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user(userDetails))
            .with(csrf())
            .param("authorId", authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 별점 범위 1점 미만(400 에러)")
  void create_fail_underMinRating() throws Exception {
    // given
    ReviewCreateRequest invalidRequest = new ReviewCreateRequest(contentId, reviewText, 0.0);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user(userDetails))
            .with(csrf())
            .param("authorId", authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 별점 범위 5점 초과(400 에러)")
  void create_fail_overMaxRating() throws Exception {
    // given
    ReviewCreateRequest invalidRequest = new ReviewCreateRequest(contentId, reviewText, 6.0);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user(userDetails))
            .with(csrf())
            .param("authorId", authorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

}
