package com.codeit.mople.domain.content.controller.api;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.dto.ContentUpdateRequest;
import com.codeit.mople.domain.content.dto.CursorResponseContentDto;
import com.codeit.mople.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(
    name = "콘텐츠 관리",
    description = "콘텐츠 관련 API"
)
public interface ContentApi {

  @Operation(
      summary = "콘텐츠 생성",
      description = "새로운 콘텐츠를 생성합니다(ADMIN 전용)"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "생성 성공",
          content = @Content(schema = @Schema(implementation = ContentResponse.class))
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
          responseCode = "403",
          description = "권한 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      )
  })
  ResponseEntity<ContentResponse> createContent(
      CustomUserDetails userDetails,
      ContentCreateRequest request,
      MultipartFile thumbnail
  );

  @Operation(
      summary = "콘텐츠 목록 조회",
      description = "커서 기반 페이지네이션으로 콘텐츠 목록을 조회합니다"
  )
  @ApiResponse(
      responseCode = "200",
      description = "성공",
      content = @Content(schema = @Schema(implementation = CursorResponseContentDto.class))
  )
  ResponseEntity<CursorResponseContentDto> getContents(
      UUID cursorId,
      Instant cursorCreatedAt,
      int limit
  );

  @Operation(
      summary = "콘텐츠 단건 조회",
      description = "콘텐츠 ID를 통해 상세 정보를 조회합니다"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(schema = @Schema(implementation = ContentResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "콘텐츠를 찾을 수 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      )
  })
  ResponseEntity<ContentResponse> getContent(
      UUID contentId
  );

  @Operation(
      summary = "콘텐츠 수정",
      description = "기존 콘텐츠 정보를 수정합니다(ADMIN 전용)"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "수정 성공",
          content = @Content(schema = @Schema(implementation = ContentResponse.class))
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "콘텐츠를 찾을 수 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      )
  })
  ResponseEntity<ContentResponse> updateContent(
      UUID contentId,
      ContentUpdateRequest request,
      MultipartFile thumbnail
  );

  @Operation(
      summary = "콘텐츠 삭제",
      description = "콘텐츠를 삭제합니다(ADMIN 전용)"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "삭제 성공"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      )
  })
  ResponseEntity<Void> deleteContent(
      UUID contentId
  );

  @Operation(
      summary = "콘텐츠 시청 세션 목록 조회",
      description = "특정 콘텐츠를 시청 중인 세션 목록을 조회합니다",
      tags = {"시청 세션 관리"}
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(schema = @Schema(implementation = CursorResponseWatchingSessionDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 커서 또는 정렬 조건",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "콘텐츠를 찾을 수 없음",
          content = @Content(schema = @Schema(implementation = com.codeit.mople.global.response.ApiResponse.class))
      )
  })
  ResponseEntity<CursorResponseWatchingSessionDto> getWatchingSessions(
      UUID contentId,
      String watcherNameLike,
      String cursor,
      UUID idAfter,
      int limit,
      String sortDirection,
      String sortBy
  );
}
