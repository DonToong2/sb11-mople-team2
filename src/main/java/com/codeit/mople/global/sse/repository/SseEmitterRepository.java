package com.codeit.mople.global.sse.repository;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SseEmitterRepository {

  // TODO 김명근: 동시 로그인이 허용되는지 파악 후 Set 집합으로 SseEmitter 모을지 단일로 할지 결정
  private final ConcurrentHashMap<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public void save(UUID receiverId, SseEmitter emitter) {
    // receiverId가 처음 들어왔으면 ConcurrentHashMap 동작을 기반으로 하는 새로운 Set을 생성하여 emitter를 Set에 삽입
    // 그 이후로는 Set에 emitter를 삽입
    emitters.computeIfAbsent(
        receiverId,
        key -> ConcurrentHashMap.newKeySet()
    ).add(emitter);
  }

  // receiverId에 해당하는 모든 SseEmitter를 반환
  // receiverId에 해당하는 연결이 없으면 빈 Set을 반환
  public Set<SseEmitter> findAll(UUID receiverId) {
    return emitters.getOrDefault(receiverId, Collections.emptySet());
  }

  public void remove(UUID receiverId, SseEmitter emitter) {

    // receiverId에 대한 연결들
    Set<SseEmitter> userEmitters = emitters.get(receiverId);

    // 비어있다면 삭제하지 않고 스킵
    if (userEmitters == null) {
      return;
    }
    
    userEmitters.remove(emitter);

    // 삭제 후 Set이 비어있다면 receiverId 키 삭제
    if (userEmitters.isEmpty()) {
      // 다른 스레드에서 연결 추가 됐을 경우를 고려하여 value가 userEmitters일때만 receiverId key 삭제
      emitters.remove(receiverId, userEmitters);
    }
  }

}
