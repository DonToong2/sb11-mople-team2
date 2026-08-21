package com.codeit.mople.domain.content.client.sportsdb.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.ContentRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
class SportsDbItemWriterTest {

  @Mock
  private ContentRepository contentRepository;

  @InjectMocks
  private SportsDbItemWriter writer;

  //saveAll에 전달되는 파라미터 리스트를 캡처하기 위한 ArgumentCaptor
  @Captor
  private ArgumentCaptor<List<Content>> saveAllCaptor;

  @Test
  @DisplayName("모두 새로운 데이터인 경우 전부 저장(saveAll)된다")
  void write_Success_AllNewContents() throws Exception {
    Content content1 = mock(Content.class);
    given(content1.getExternalId()).willReturn("EXT-1");
    Content content2 = mock(Content.class);
    given(content2.getExternalId()).willReturn("EXT-2");

    Chunk<Content> chunk = new Chunk<>(List.of(content1, content2));

    //DB에 존재하는 외부 ID가 없다고 모킹
    given(contentRepository.findByTypeAndExternalIdIn(eq(ContentType.SPORT), anyList()))
        .willReturn(List.of());

    writer.write(chunk);

    //ArgumentCaptor를 통해 저장된 리스트 캡처
    verify(contentRepository).saveAll(saveAllCaptor.capture());
    List<Content> savedContents = saveAllCaptor.getValue();

    //캡처한 리스트에 두 객체가 정확히 모두 포함되어 저장 요청되었는지 검증
    assertThat(savedContents).containsExactlyInAnyOrder(content1, content2);
  }

  @Test
  @DisplayName("일부 데이터가 중복인 경우 새로운 데이터만 필터링되어 저장된다")
  void write_Success_PartialDuplication() throws Exception {
    //Chunk로 들어온 데이터(입력값)와 DB에서 꺼내온 데이터를 별개의 Mock으로 분리
    Content inputOldContent = mock(Content.class);
    given(inputOldContent.getExternalId()).willReturn("EXT-OLD");
    Content repoOldContent = mock(Content.class);
    given(repoOldContent.getExternalId()).willReturn("EXT-OLD");

    Content newContent = mock(Content.class);
    given(newContent.getExternalId()).willReturn("EXT-NEW");

    lenient().when(newContent.getTitle()).thenReturn("새 경기");

    Chunk<Content> chunk = new Chunk<>(List.of(newContent, inputOldContent));

    //DB에 "EXT-OLD"가 이미 존재한다고 모킹 (DB 엔티티 반환)
    given(contentRepository.findByTypeAndExternalIdIn(eq(ContentType.SPORT), anyList()))
        .willReturn(List.of(repoOldContent));

    writer.write(chunk);

    //기존 데이터(DB 엔티티)는 갱신되는지 검증
    verify(repoOldContent).updateContentInfo(any(), any(), any(), any());

    //ArgumentCaptor를 통해 저장된 리스트 검증
    verify(contentRepository).saveAll(saveAllCaptor.capture());
    List<Content> savedContents = saveAllCaptor.getValue();

    //저장 리스트에 입력된 중복 객체(inputOldContent)가 버려지고 DB 객체(repoOldContent)가 들어가 있는지 검증
    assertThat(savedContents).containsExactlyInAnyOrder(newContent, repoOldContent);
  }

  @Test
  @DisplayName("모든 데이터가 중복인 경우 기존 엔티티만 갱신되어 저장된다")
  void write_Success_AllDuplicated() throws Exception {
    //입력값과 DB값을 분리
    Content inputOldContent = mock(Content.class);
    given(inputOldContent.getExternalId()).willReturn("EXT-OLD");
    Content repoOldContent = mock(Content.class);
    given(repoOldContent.getExternalId()).willReturn("EXT-OLD");

    Chunk<Content> chunk = new Chunk<>(List.of(inputOldContent));

    given(contentRepository.findByTypeAndExternalIdIn(eq(ContentType.SPORT), anyList()))
        .willReturn(List.of(repoOldContent));

    writer.write(chunk);

    //모든 데이터가 중복이어도 기존 객체의 최신 정보 갱신(updateContentInfo) 후 DB 저장이 호출되는지 검증
    verify(repoOldContent).updateContentInfo(any(), any(), any(), any());

    verify(contentRepository).saveAll(saveAllCaptor.capture());
    List<Content> savedContents = saveAllCaptor.getValue();

    //DB에서 조회한 기존 객체만 리스트에 담겨 저장되어야 함
    assertThat(savedContents).containsExactly(repoOldContent);
  }
}