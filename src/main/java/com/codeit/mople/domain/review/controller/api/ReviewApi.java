package com.codeit.mople.domain.review.controller.api;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.mople.domain.review.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
    name = "리뷰 관리",
    description = "리뷰 관련 API"
)
public interface ReviewApi {

  @Operation(
      summary = "리뷰 생성",
      description = "생성한 리뷰는 API 요청자 본인의 리뷰로 생성됩니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "리뷰 생성 성공",
          content = @Content(schema = @Schema(implementation = ReviewResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 오류",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "콘텐츠를 찾을 수 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      ),
  })
  ResponseEntity<ReviewResponse> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody ReviewCreateRequest request
  );
}
