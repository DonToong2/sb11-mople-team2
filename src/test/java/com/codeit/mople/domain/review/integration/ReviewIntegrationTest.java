package com.codeit.mople.domain.review.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import com.codeit.mople.domain.review.entity.Review;
import com.codeit.mople.domain.review.repository.ReviewRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Import(SecurityConfig.class)
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class ReviewIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  private User savedAuthor;
  private Content savedContent;
  private ReviewCreateRequest request;
  private String reviewText;
  private Double reviewRating;

  @BeforeEach
  void setUp() {
    savedAuthor = userRepository.save(
        User.createUser("test@test.com", "12345678", "test")
    );
    savedContent = contentRepository.save(new Content(
            ContentType.DRAMA,
            "test",
            "test 컨텐츠",
            "test/image.png",
            List.of("테스트")
        )
    );

    reviewText = "리뷰 내용";
    reviewRating = 4.0;

    request = new ReviewCreateRequest(savedContent.getId(), reviewText, reviewRating);
  }

  @Test
  @DisplayName("리뷰 생성 성공")
  void create_success() throws Exception {
    // given

    // BeforeEach에서 savedAuthor, savedContent, request 초기화

    // when & then
    MvcResult result = mockMvc.perform(post("/api/reviews")
            .with(user("test"))
            .with(csrf())
            .param("authorId", savedAuthor.getId().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.contentId").value(savedContent.getId().toString()))
        .andExpect(jsonPath("$.author.userId").value(savedAuthor.getId().toString()))
        .andExpect(jsonPath("$.author.name").value(savedAuthor.getName()))
        .andExpect(jsonPath("$.author.profileImageUrl").value(savedAuthor.getProfileImageUrl()))
        .andExpect(jsonPath("$.text").value(reviewText))
        .andExpect(jsonPath("$.rating").value(reviewRating))
        .andReturn();

    // 응답 추출
    ReviewResponse response = objectMapper.readValue(
        result.getResponse().getContentAsString(), ReviewResponse.class
    );

    // 헤더 검증
    assertThat(result.getResponse().getHeader("Location"))
        .isEqualTo("/api/reviews/" + response.id());

    // DB 검증
    Review savedReview = reviewRepository.findById(response.id()).orElseThrow();

    assertThat(savedReview.getContent().getId()).isEqualTo(savedContent.getId());
    assertThat(savedReview.getAuthor().getId()).isEqualTo(savedAuthor.getId());
    assertThat(savedReview.getText()).isEqualTo(reviewText);
    assertThat(savedReview.getRating()).isEqualTo(reviewRating);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 컨텐츠가 존재하지 않음(404 에러)")
  void create_fail_notFoundContent() throws Exception {
    // given
    UUID notExistContentId = UUID.randomUUID();

    ReviewCreateRequest invalidRequest =
        new ReviewCreateRequest(notExistContentId, reviewText, reviewRating);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user("test"))
            .with(csrf())
            .param("authorId", savedAuthor.getId().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isNotFound());

    assertThat(reviewRepository.count()).isZero();
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 사용자가 존재하지 않음(404 에러)")
  void create_fail_notFoundUser() throws Exception {
    // given
    UUID notExistAuthorId = UUID.randomUUID();

    // BeforeEach에서 request 초기화

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user("test"))
            .with(csrf())
            .param("authorId", notExistAuthorId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound());

    assertThat(reviewRepository.count()).isZero();
  }
}
