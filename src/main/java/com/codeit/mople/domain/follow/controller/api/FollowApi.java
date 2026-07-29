package com.codeit.mople.domain.follow.controller.api;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.follow.dto.FollowRequest;
import com.codeit.mople.domain.follow.dto.FollowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "팔로우 관리")
public interface FollowApi {

  // ErrorResponse 는 어떻게 해야할지??, 일단 작업하던거 하고 수정할 예정
  @Operation(operationId = "createFollow", summary = "팔로우")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "성공",
          content = @Content(schema = @Schema(implementation = FollowResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 오류",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 오류",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))),
  })
  public ResponseEntity<FollowResponse> createFollow(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
      @Valid @RequestBody FollowRequest followRequest);
}
