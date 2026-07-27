package com.codeit.mople.domain.content.service;

import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.ContentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

  private final ContentRepository contentRepository;

  @Override
  @Transactional
  public ContentResponse createContent(UUID adminId, ContentCreateRequest request,
      MultipartFile thumbnail) {
    
    //TODO: 관리자 권한 검증 추가 예정

    //썸네일 이미지 업로드 처리(현재는 임시 URL 처리)
    String uploadedThumbnailUrl = null;
    if (thumbnail != null && !thumbnail.isEmpty()) {
      //TODO: 추후 AWS S3 등에 업로드하고 반한된 URL 사용 예정
      uploadedThumbnailUrl = "http://example.com/images/" + thumbnail.getOriginalFilename();
    }

    //Request DTO 데이터를 바탕으로 Content 엔티티 생성
    Content content = new Content(
        ContentType.valueOf(request.type().toUpperCase()),
        request.title(),
        request.description(),
        uploadedThumbnailUrl,
        request.tags()
    );

    //DB에 엔티티 저장
    Content savedContent = contentRepository.save(content);

    //저장된 엔티티 데이터를 ContentResponse 구조로 변환하여 반환
    return new ContentResponse(
        savedContent.getId(),
        savedContent.getType().name(),
        savedContent.getTitle(),
        savedContent.getDescription(),
        savedContent.getThumbnailUrl(),
        savedContent.getTags(),
        savedContent.getAverageRating(),
        savedContent.getReviewCount(),
        savedContent.getWatcherCount()
    );
  }
}
