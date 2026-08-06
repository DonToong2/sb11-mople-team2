package com.codeit.mople.global.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mople.global.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
public class SseServiceTest {

  @Mock
  private SseEmitterRepository emitterRepository;

  @InjectMocks
  private SseService sseService;

  private UUID receiverId;
  private SseEmitter emitter1;
  private SseEmitter emitter2;

  @BeforeEach
  void setUp() {
    receiverId = UUID.randomUUID();
    emitter1 = mock(SseEmitter.class);
    emitter2 = mock(SseEmitter.class);
  }

  @Nested
  @DisplayName("SSE 연결")
  class Connect {

    @Test
    @DisplayName("SSE 연결 성공")
    void connect_success() {
      // given

      // BeforeEach에서 receiverId를 초기화

      // when
      SseEmitter result = sseService.connect(receiverId);

      // then
      assertThat(result).isNotNull();

      // 레포지토리 호출 검증
      verify(emitterRepository).save(receiverId, result);
    }

  }

  @Nested
  @DisplayName("SSE 이벤트 전송")
  class Send {

    @Test
    @DisplayName("SSE 이벤트 전송 성공 - 사용자의 단일 연결")
    void send_success() throws IOException {
      // given

      // BeforeEach에서 receiverId, emitter1를 초기화

      when(emitterRepository.findAll(receiverId))
          .thenReturn(Set.of(emitter1));

      // when
      sseService.send(receiverId, "eventName", "data");

      // then
      verify(emitter1).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("SSE 이벤트 전송 성공 - 사용자의 다중 연결")
    void send_success_multiple() throws IOException {
      // given

      // BeforeEach에서 receiverId, emitter1, emitter2를 초기화

      when(emitterRepository.findAll(receiverId))
          .thenReturn(Set.of(emitter1, emitter2));

      // when
      sseService.send(receiverId, "eventName", "data");

      // then
      verify(emitter1).send(any(SseEmitter.SseEventBuilder.class));
      verify(emitter2).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("SSE 이벤트 전송 무시 - 사용자의 연결이 없는 경우")
    void send_ignore_noEmitter() {
      // given
      
      // BeforeEach에서 receiverId를 초기화

      when(emitterRepository.findAll(receiverId))
          .thenReturn(Set.of());

      // when
      sseService.send(receiverId, "eventName", "data");

      // then
      verify(emitterRepository).findAll(receiverId);
    }

    @Test
    @DisplayName("SSE 이벤트 전송 실패 - 전송 실패한 연결 제거")
    void send_fail_remove() throws IOException {
      // given

      // BeforeEach에서 receiverId, emitter1를 초기화

      when(emitterRepository.findAll(receiverId))
          .thenReturn(Set.of(emitter1));

      // sseService.send() 처리 중 IOException 발생
      doThrow(
          new IOException())
          .when(emitter1)
          .send(any(SseEmitter.SseEventBuilder.class)
          );

      // when
      sseService.send(receiverId, "eventName", "data");

      // then
      // 전송시도는 되지만 받지 못하고 삭제됨
      verify(emitterRepository).remove(receiverId, emitter1);

      verify(emitter1).completeWithError(any(IOException.class));
    }

    @Test
    @DisplayName("SSE 이벤트 전송 실패 - 여러 연결 중 하나만 실패")
    void send_fail_remove_one() throws IOException {
      // given

      // BeforeEach에서 receiverId, emitter1, emitter2를 초기화

      when(emitterRepository.findAll(receiverId))
          .thenReturn(Set.of(emitter1, emitter2));

      // emitter2에서 IOException 발생
      doThrow(new IOException())
          .when(emitter2)
          .send(any(SseEmitter.SseEventBuilder.class));

      // when
      sseService.send(receiverId, "eventName", "data");

      // then
      // emitter1, emitter2에 이벤트를 전송(시도)하고 emitter2 연결은 삭제되어야 함
      verify(emitter1).send(any(SseEmitter.SseEventBuilder.class));
      verify(emitter2).send(any(SseEmitter.SseEventBuilder.class));
      
      // 레포지토리 호출 검증
      verify(emitterRepository).remove(receiverId, emitter2);
      verify(emitterRepository, never()).remove(receiverId, emitter1);
    }

  }

}
