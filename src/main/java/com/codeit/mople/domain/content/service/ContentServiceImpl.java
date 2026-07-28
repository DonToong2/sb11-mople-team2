package com.codeit.mople.domain.content.service;

import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentPageResponse;
import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.exception.InvalidContentTypeException;
import com.codeit.mople.domain.content.exception.InvalidPageRequestException;
import com.codeit.mople.domain.content.mapper.ContentMapper;
import com.codeit.mople.domain.content.repository.ContentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

  private final ContentRepository contentRepository;
  private final ContentMapper contentMapper;

  //콘텐츠 생성 로직
  @Override
  @Transactional
  public ContentResponse createContent(UUID adminId, ContentCreateRequest request,
      MultipartFile thumbnail) {

    //ContentType 변환 방어 로직
    ContentType contentType;
    try {
      contentType = ContentType.valueOf(request.type().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidContentTypeException();
    }

    //TODO: 관리자 권한 검증 추가 예정

    //썸네일 이미지 업로드 처리(현재는 임시 URL 처리)
    String uploadedThumbnailUrl = null;
    if (thumbnail != null && !thumbnail.isEmpty()) {
      //TODO: 추후 AWS S3 등에 업로드하고 반한된 URL 사용 예정
      uploadedThumbnailUrl = "http://example.com/images/" + thumbnail.getOriginalFilename();
    }

    //Request DTO 데이터를 바탕으로 매퍼를 통해 Content 엔티티 생성
    Content content = contentMapper.toEntity(request, contentType, uploadedThumbnailUrl);

    //DB에 엔티티 저장
    Content savedContent = contentRepository.save(content);

    //저장된 엔티티 데이터를 ContentResponse 구조로 변환하여 반환
    return contentMapper.toDto(savedContent);
  }

  //콘텐츠 목록 조회 로직
  @Override
  @Transactional(readOnly = true)
  public ContentPageResponse getContents(int limit, String sortDirection, String sortBy) {
    //Limit 검증 로직
    if (limit <= 0) {
      throw new InvalidPageRequestException();
    }

    //정렬 방향 설정(ASCENDING(오름차순) 또는 DESCENDING(내림차순))
    Sort.Direction direction = sortDirection.equalsIgnoreCase("DESCENDING")
        ? Direction.DESC
        : Direction.ASC;

    //PageRequest 객체 생성(첫 페이지(0) 부터 limit 개수만큼 조회)
    PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(direction, sortBy));

    //DB 조회
    Page<Content> contentPage = contentRepository.findAll(pageRequest);

    //Content 엔티티 리스트를 ContentResponse DTO 리스트로 변환
    List<ContentResponse> contentResponses = contentPage.getContent().stream()
        .map(contentMapper::toDto).toList();

    //ContentPageResponse에 맞춰 매퍼를 통해 페이징 응답 객체 생성 및 반환
    return contentMapper.toPageResponse(contentResponses, contentPage, sortBy, sortDirection);
  }

  @Override
  public ContentResponse getContent(UUID contentId) {
    return null;
  }
}
