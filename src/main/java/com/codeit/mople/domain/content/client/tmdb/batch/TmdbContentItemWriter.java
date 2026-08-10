package com.codeit.mople.domain.content.client.tmdb.batch;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.codeit.mople.domain.content.repository.ContentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@Slf4j
public class TmdbContentItemWriter implements ItemWriter<Content> {

  private final ContentRepository contentRepository;
  private final ContentType contentType;
  private final Counter insertedCounter;
  private final Counter updatedCounter;
  private final Counter unchangedCounter;

  public TmdbContentItemWriter(
      ContentRepository contentRepository,
      ContentType contentType,
      MeterRegistry meterRegistry) {
    this.contentRepository = contentRepository;
    this.contentType = contentType;
    this.insertedCounter = counter(meterRegistry, contentType, "inserted");
    this.updatedCounter = counter(meterRegistry, contentType, "updated");
    this.unchangedCounter = counter(meterRegistry, contentType, "unchanged");
  }

  // 생성자에 붙는 메트릭
  private static Counter counter(
      MeterRegistry meterRegistry, ContentType contentType, String result) {
    return Counter.builder("batch.tmdb.upsert")
        .description("TMDB 수집 배치 upsert 결과 건수")
        .tag("type", contentType.name())
        .tag("result", result)
        .register(meterRegistry);
  }


  @Override
  public void write(@NonNull Chunk<? extends Content> chunk) throws Exception {
    // 청크 할당
    List<? extends Content> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    // 하나의 청크 단위에 동일 externalId가 있으면 데이터를 안넣음 / 없으면 넣음
    Map<String, Content> incoming = new LinkedHashMap<>();
    for (Content item : items) {
      incoming.putIfAbsent(item.getExternalId(), item);
    }

    // 청크에 있는 데이터와 일치하는 현재 DB데이터
    Map<String, Content> stored = contentRepository
        .findByTypeAndExternalIdIn(contentType, List.copyOf(incoming.keySet()))
        .stream()
        .collect(Collectors.toMap(Content::getExternalId, Function.identity()));

    List<Content> toInsert = new ArrayList<>();
    int updated = 0;
    int unchanged = 0;

    for (Content item : incoming.values()) {
      // 청크에 있는 데이터와 일치하는 현재 DB데이터의 값을 found에 저장
      Content found = stored.get(item.getExternalId());

      if (found == null) {
        toInsert.add(item);
        continue;
      }

      boolean changed = found.syncFromExternal(
          item.getTitle(), item.getDescription(), item.getThumbnailUrl(), item.getTags());

      if (changed) {
        updated++;
      } else  {
        unchanged++;
      }
    }

    int inserted = toInsert.isEmpty() ? 0 : contentRepository.saveAll(toInsert).size();

    insertedCounter.increment(inserted);
    updatedCounter.increment(updated);
    unchangedCounter.increment(unchanged);

    log.info("TMDB 수집({}), 전달={}, 신규={}, 갱신={}, 무변화={}",
        contentType, items.size(), inserted, updated, unchanged);
  }
}
