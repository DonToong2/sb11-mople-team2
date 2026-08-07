package com.codeit.mople.global.sse.repository;

import com.codeit.mople.global.sse.model.SseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Repository;

@Repository
public class SseEventRepository {

  private final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<SseEvent>> events =
      new ConcurrentHashMap<>();

  public void save(SseEvent event) {
    // receiverId가 처음 들어왔을 경우 ConcurrentLinkedQueue 생성
    // 이후 Value에 event 삽입
    events.computeIfAbsent(
        event.receiverId(),
        key -> new ConcurrentLinkedQueue<>()
    ).add(event);
  }

  public List<SseEvent> findAfter(UUID receiverId, UUID lastEventId) {
    // receiverId에 해당하는 value(이벤트 저장 큐)들을 가져옴(순서 보장O)
    ConcurrentLinkedQueue<SseEvent> userEvents = events.get(receiverId);

    // 비어있을 경우 빈 List 반환
    if (userEvents == null) {
      return List.of();
    }

    // 결과를 반환 리스트 생성
    List<SseEvent> result = new ArrayList<>();

    // lastEventId 이후 값들을 찾기 위한 flag
    boolean found = false;

    for (SseEvent event : userEvents) {
      // found가 true일 경우 리스트에 추가(lastEventId 이후 데이터를 추가)
      if (found) {
        result.add(event);
      }

      // lastEventId와 이벤트ID가 일치할 경우 위치를 발견함
      if (event.id().equals(lastEventId)) {
        found = true;
      }
    }

    return result;
  }
}
