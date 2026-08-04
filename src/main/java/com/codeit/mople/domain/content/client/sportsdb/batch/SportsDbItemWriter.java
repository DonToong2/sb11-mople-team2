package com.codeit.mople.domain.content.client.sportsdb.batch;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.repository.ContentRepository;
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

  //변환된 엔티티를 DB에 일괄 저장
  @Override
  public void write(Chunk<? extends Content> chunk) {
    log.info("Content DB 저장 시작 - Chunk 사이즈: {}건", chunk.getItems().size());

    //Processor를 통과한 유효한 데이터들을 DB에 한 번에 저장
    contentRepository.saveAll(chunk.getItems());

    log.info("Content DB 저장 완료");
  }
}
