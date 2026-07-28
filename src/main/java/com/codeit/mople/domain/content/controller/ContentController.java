package com.codeit.mople.domain.content.controller;

import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentPageResponse;
import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.service.ContentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {
  private final ContentService contentService;

  //콘텐츠 생성
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentResponse> createContent(
      @RequestHeader("X-User-Id")UUID adminId,
      @Valid @RequestPart("request")ContentCreateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
    ContentResponse response = contentService.createContent(adminId, request, thumbnail);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  //콘텐츠 목록 조회
  @GetMapping
  public ResponseEntity<ContentPageResponse> getContents(
      @RequestParam("limit") int limit,
      @RequestParam("sortDirection") String sortDirection,
      @RequestParam("sortBy") String sortBy) {
    ContentPageResponse response = contentService.getContents(limit, sortDirection, sortBy);
    return ResponseEntity.ok(response);
  }

  //콘텐츠 단건 조회
  @GetMapping("/{contentId}")
  public ResponseEntity<ContentResponse> getContent(
      @PathVariable UUID contentId) {
    ContentResponse response = contentService.getContent(contentId);
    return ResponseEntity.ok(response);
  }
}
