package com.codeit.mople.global.sse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseEmitterRepositoryTest {

  private SseEmitterRepository repository;

  private UUID receiverId;
  private SseEmitter sseEmitter1;
  private SseEmitter sseEmitter2;

  @BeforeEach
  void setUp() {
    repository = new SseEmitterRepository();

    receiverId = UUID.randomUUID();
    sseEmitter1 = new SseEmitter();
    sseEmitter2 = new SseEmitter();
  }

  @Nested
  @DisplayName("SseEmitter 저장")
  class Save {

    @Test
    @DisplayName("SseEmitter 저장 성공 - 단일 SseEmitter 저장")
    void save_success() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1를 초기화

      // when
      repository.save(receiverId, sseEmitter1);

      // then
      assertThat(repository.findAll(receiverId))
          .containsExactlyInAnyOrder(sseEmitter1);
    }

    @Test
    @DisplayName("SseEmitter 저장 성공 - 여러 개의 SseEmitter 저장")
    void save_success_multiple() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1, sseEmitter2를 초기화

      // when
      repository.save(receiverId, sseEmitter1);
      repository.save(receiverId, sseEmitter2);

      // then
      assertThat(repository.findAll(receiverId))
          .containsExactlyInAnyOrder(sseEmitter1, sseEmitter2);
    }
    
    @Test
    @DisplayName("SseEmitter 저장 성공 - 중복 SseEmitter 저장 시 하나만 저장")
    void save_success_duplicate() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1를 초기화

      // when
      repository.save(receiverId, sseEmitter1);
      repository.save(receiverId, sseEmitter1);

      // then
      assertThat(repository.findAll(receiverId))
          .containsExactlyInAnyOrder(sseEmitter1);
    }

  }

  @Nested
  @DisplayName("SseEmitter 목록 조회")
  class FindAll {
    
    @Test
    @DisplayName("SseEmitter 목록 조회 성공")
    void findAll_success() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1, sseEmitter2를 초기화
      repository.save(receiverId, sseEmitter1);
      repository.save(receiverId, sseEmitter2);

      // when
      Set<SseEmitter> result = repository.findAll(receiverId);

      // then
      assertThat(result)
          .containsExactlyInAnyOrder(sseEmitter1, sseEmitter2);
    }
    
    @Test
    @DisplayName("SseEmitter 목록 조회 성공 - receiverId가 존재하지 않을 경우 빈 Set 반환")
    void findAll_success_empty() {
      // given

      // BeforeEach에서 receiverId를 초기화

      // when
      Set<SseEmitter> result = repository.findAll(receiverId);

      // then
      assertThat(result).isEmpty();
    }

  }

  @Nested
  @DisplayName("SseEmitter 삭제")
  class Delete {
    
    @Test
    @DisplayName("SseEmitter 삭제 성공")
    void delete_success() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1, sseEmitter2를 초기화

      repository.save(receiverId, sseEmitter1);
      repository.save(receiverId, sseEmitter2);

      // when
      repository.remove(receiverId, sseEmitter1);

      // then
      assertThat(repository.findAll(receiverId))
          .containsExactlyInAnyOrder(sseEmitter2);
    }
    
    @Test
    @DisplayName("SseEmitter 삭제 성공 - 삭제 이후 Set이 비어있을 경우 receiverId 연결 정보 삭제")
    void delete_success_empty() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1를 초기화

      repository.save(receiverId, sseEmitter1);

      // when
      repository.remove(receiverId, sseEmitter1);

      // then
      assertThat(repository.findAll(receiverId)).isEmpty();
    }
    
    @Test
    @DisplayName("SseEmitter 삭제 무시 - receiverId가 존재하지 않을 경우 동작하지 않음")
    void delete_ignore_notFoundReceiverId() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1를 초기화

      // receiverId가 존재하지 않는 경우를 증명하기 위해 (receiverId, sseEmitter1)를 save하지 않음

      // when
      repository.remove(receiverId, sseEmitter1);

      // then
      assertThat(repository.findAll(receiverId)).isEmpty();
    }

    @Test
    @DisplayName("SseEmitter 삭제 무시 - SseEmitter가 존재하지 않을 경우")
    void delete_ignore_notFoundEmitter() {
      // given

      // BeforeEach에서 receiverId, sseEmitter1, sseEmitter2를 초기화

      // sseEmitter2는 저장하지 않음
      repository.save(receiverId, sseEmitter1);

      // when
      repository.remove(receiverId, sseEmitter2);

      // then
      assertThat(repository.findAll(receiverId))
          .containsExactlyInAnyOrder(sseEmitter1);
    }

  }

}
