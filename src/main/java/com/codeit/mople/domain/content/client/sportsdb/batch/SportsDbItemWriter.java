package com.codeit.mople.domain.content.client.sportsdb.batch;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.ContentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportsDbItemWriter implements ItemWriter<Content> {

  private final ContentRepository contentRepository;

  @Override
  public void write(Chunk<? extends Content> chunk) {
    log.info("Content DB 저장 시작 - Chunk 사이즈: {}건", chunk.getItems().size());

    //c로 캐스팅하거나 map을 통해 Content 타입으로 명시
    List<Content> items = chunk.getItems().stream()
        .map(c -> (Content) c)
        .toList();

    //이번 Chunk의 외부 식별자 추출
    List<String> externalIds = items.stream()
        .map(Content::getExternalId)
        .toList();

    //DB에 이미 존재하는 엔티티 목록 조회 및 Map 매핑
    Map<String, Content> existingContentMap = contentRepository
        .findByTypeAndExternalIdIn(ContentType.SPORT, externalIds).stream()
        .collect(Collectors.toMap(Content::getExternalId, Function.identity()));

    List<Content> toSave = new ArrayList<>();
    int updatedCount = 0;
    int insertedCount = 0;

    for (Content item : items) {
      Content existing = existingContentMap.get(item.getExternalId());
      if (existing != null) {
        //기존 경기 정보가 있으면 최신 정보로 갱신
        existing.updateContentInfo(
            item.getTitle(),
            item.getDescription(),
            item.getThumbnailUrl(),
            item.getTags()
        );
        toSave.add(existing);
        updatedCount++;
      } else {
        //새로운 경기 데이터면 신규 등록
        toSave.add(item);
        insertedCount++;
      }
    }

    if (!toSave.isEmpty()) {
      contentRepository.saveAll(toSave);
      log.info("스포츠 경기 데이터 처리 완료 - 신규 등록: {}건, 기존 갱신: {}건", insertedCount, updatedCount);
    } else {
      log.info("저장할 새로운 경기 데이터가 없습니다");
    }
  }
}