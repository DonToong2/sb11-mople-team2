package com.codeit.mople.domain.playlist.controller.api;

import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
    name = "플레이리스트 관리",
    description = "플레이리스트 관련 API"
)
public interface PlaylistApi {

  @Operation(
      summary = "플레이리스트 생성",
      description = "생성한 플레이리스트는 API 요청자 본인의 플레이리스트로 생성됩니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "성공",
          content = @Content(schema = @Schema(implementation = PlaylistResponse.class))
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
          description = "플레이리스트 소유자를 찾을 수 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      )
  })
  ResponseEntity<PlaylistResponse> create(
      @RequestParam UUID ownerId,
      @Valid @RequestBody PlaylistCreateRequest request
  );
}
