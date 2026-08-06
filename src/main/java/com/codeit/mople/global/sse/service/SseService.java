package com.codeit.mople.global.sse.service;

import com.codeit.mople.global.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

  private static final long TIMEOUT = 60 * 60 * 1000L; // 1시간

  private final SseEmitterRepository emitterRepository;

  public SseEmitter connect(UUID receiverId) {
    SseEmitter emitter = new SseEmitter(TIMEOUT);

    // 입력, 반환 둘 다 없음(Runnable)
    emitter.onCompletion(() ->  {
      log.debug("SSE 연결 종료 - receiverId={}", receiverId);
      emitterRepository.remove(receiverId, emitter);
    });
    emitter.onTimeout(() -> {
      log.debug("SSE 연결 시간 초과 - receiverId={}", receiverId);
      emitterRepository.remove(receiverId, emitter);
    });

    // Consumer(void)
    emitter.onError(throwable -> {
      log.warn("SSE 연결 오류 발생 - receiverId={}", receiverId, throwable);
      emitterRepository.remove(receiverId, emitter);
    });

    emitterRepository.save(receiverId, emitter);

    return emitter;
  }

  public void send(UUID receiverId, String eventName, Object data) {
    Set<SseEmitter> emitters = emitterRepository.findAll(receiverId);

    for (SseEmitter emitter : emitters) {

      try {
        emitter.send(
            SseEmitter.event()
                .id(UUID.randomUUID().toString())
                .name(eventName)
                .data(data)
        );
      } catch (IOException e) {
        log.warn("SSE 전송 실패 receiverId={}", receiverId, e);

        emitterRepository.remove(receiverId, emitter);

        emitter.completeWithError(e);
      }
    }
  }

}
