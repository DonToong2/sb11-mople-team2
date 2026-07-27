package com.codeit.mople.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentPageResponse;
import com.codeit.mople.domain.content.dto.ContentResponse;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.mapper.ContentMapper;
import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.exception.ContentErrorCode;
import com.codeit.mople.global.error.CustomException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
public class ContentServiceImplTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ContentMapper contentMapper;

  @InjectMocks
  private ContentServiceImpl contentService;

  //=========================================================================================
  //콘텐츠 생성 테스트
  //=========================================================================================

  @Test
  @DisplayName("콘텐츠 생성 성공 - 레포지토리 저장 및 DTO 변환 정상적 수행")
  void createContent_Success() {
    UUID adminId = UUID.randomUUID();
    ContentCreateRequest request = new ContentCreateRequest("MOVIE", "테스트 영화",
        "설명", List.of("액션"));
    MockMultipartFile thumbnail = new MockMultipartFile("thumbnail",
        "test.png", "image/png", "dummy".getBytes());

    Content savedContent = new Content(ContentType.MOVIE, "테스트 영화", "설명",
        "http://example.com/images/test.png", List.of("액션"));
    ContentResponse expectedResponse = new ContentResponse(UUID.randomUUID(), "MOVIE",
        "테스트 영화", "설명", "http://example.com/images/test.png",
        List.of("액션"), 0.0, 0, 0L);

    given(contentRepository.save(any(Content.class))).willReturn(savedContent);
    given(contentMapper.toDto(savedContent)).willReturn(expectedResponse);

    ContentResponse response = contentService.createContent(adminId, request,  thumbnail);

    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo("테스트 영화");
    verify(contentRepository).save(any(Content.class));
  }

  @Test
  @DisplayName("콘텐츠 생성 실패 - 잘못된 ContentType 전달 시 IllegalArgumentException 발생")
  void createContent_Fail_InvalidType() {
    UUID adminId = UUID.randomUUID();
    ContentCreateRequest request = new ContentCreateRequest("INVALID_TYPE",
        "테스트", "설명", List.of());

    //정의 되지 않은 타입 변환 시 INVALID_CONTENT_TYPE 커스텀 예외 발생
    assertThatThrownBy(() -> contentService.createContent(adminId, request, null))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ContentErrorCode.INVALID_CONTENT_TYPE);
  }

  //=========================================================================================
  //콘텐츠 목록 조회 테스트
  //=========================================================================================

  @Test
  @DisplayName("콘텐츠 목록 조회 성공 - 요청한 조건에 맞게 페이징된 데이터 반환")
  void getContents_Success() {
    int limit = 10;
    String sortDirection = "DESCENDING";
    String sortBy = "createdAt";

    Content content1 = new Content(ContentType.MOVIE, "영화1",
        "설명", null, List.of());
    Page<Content> contentPage = new PageImpl<>(List.of(content1)); // 가짜 페이지 데이터 생성

    ContentResponse responseDto = new ContentResponse(UUID.randomUUID(),
        "MOVIE", "영화1", "설명", null, List.of(),
        0.0, 0, 0L);

    given(contentRepository.findAll(any(PageRequest.class))).willReturn(contentPage);
    given(contentMapper.toDto(content1)).willReturn(responseDto);

    ContentPageResponse response = contentService.getContents(limit, sortDirection, sortBy);

    assertThat(response).isNotNull();
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).title()).isEqualTo("영화1");
    assertThat(response.totalCount()).isEqualTo(1);
    assertThat(response.sortDirection()).isEqualTo("DESCENDING");
  }

  @Test
  @DisplayName("콘텐츠 목록 조회 실패 - limit 값이 0 이하일 경우 IllegalArgumentException 발생")
  void getContents_Fail_NegativeLimit() {
    int invalidLimit = -1;

    //PageRequest.of(0, -1) 이 실행되면서 INVALID_PAGE_REQUEST 커스텀 예외 발생
    assertThatThrownBy(() -> contentService.getContents(-1, "ASCENDING", "createdAt"))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ContentErrorCode.INVALID_PAGE_REQUEST);
  }
}
