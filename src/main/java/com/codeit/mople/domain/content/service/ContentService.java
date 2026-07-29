package com.codeit.mople.domain.content.service;

import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentPageResponse;
import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.dto.ContentUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentService {

  //콘텐츠 생성 메서드
  ContentResponse createContent(UUID adminId, ContentCreateRequest request, MultipartFile thumbnail);
  //콘텐츠 목록 조회 메서드
  ContentPageResponse getContents(int page, int limit, String sortDirection, String sortBy);
  //콘텐츠 단건 조회 메서드
  ContentResponse getContent(UUID contentId);
  //콘텐츠 수정 메서드
  ContentResponse updateContent(UUID adminId, UUID contentId, ContentUpdateRequest request, MultipartFile thumbnail);
  //콘텐츠 삭제 메서드
  void deleteContent(UUID adminId, UUID contentId);

}
