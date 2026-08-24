package com.codeit.mople.global.error;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.follow.exception.FollowConstraintErrorCodes;
import com.codeit.mople.global.response.ApiResponse;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

  private final DiscordWebhookService discordWebhookService = mock(DiscordWebhookService.class);

  private final GlobalExceptionHandler handler =
      new GlobalExceptionHandler(List.of(new FollowConstraintErrorCodes()), discordWebhookService);

  private DataIntegrityViolationException violationOf(String constraintName) {
    ConstraintViolationException cause =
        new ConstraintViolationException("중복 저장", new SQLException(), constraintName);

    return new DataIntegrityViolationException("could not execute statement", cause);
  }

  @Nested
  @DisplayName("DB 제약 위반 매핑")
  class DataIntegrityViolation {

    @Test
    @DisplayName("등록된 제약 이름이면 해당 도메인의 에러코드와 400을 반환")
    void resolveConstraintSuccess() {
      // given
      DataIntegrityViolationException exception = violationOf("uk_follows_followee_follower");

      // when
      ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(exception);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().isSuccess()).isFalse();
      assertThat(response.getBody().getError().code()).isEqualTo("FOLLOW-002");
    }

    @Test
    @DisplayName("제약 이름의 대소문자가 달라도 400 반환")
    void resolveConstraintSuccessWhenUpperCaseName() {
      // given
      DataIntegrityViolationException exception = violationOf("UK_FOLLOWS_FOLLOWEE_FOLLOWER");

      // when
      ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(exception);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getError().code()).isEqualTo("FOLLOW-002");
    }

    @Test
    @DisplayName("등록되지 않은 제약 이름이면 500을 반환")
    void resolveConstraintFailWhenNotRegistered() {
      // given
      DataIntegrityViolationException exception = violationOf("uk_something_else");

      // when
      ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(exception);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("제약 이름을 알 수 없으면 500을 반환")
    void resolveConstraintFailWhenNameIsMissing() {
      // given
      DataIntegrityViolationException exception =
          new DataIntegrityViolationException("could not execute statement");

      // when
      ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(exception);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}