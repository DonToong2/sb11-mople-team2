package com.codeit.mople.domain.directmessage.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DirectMessageReadRedisRepositoryTest {

  @InjectMocks
  private DirectMessageReadRedisRepository repository;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(repository, "chunkSize", 500L);
  }

  @Test
  @DisplayName("성공: Lua 스크립트 실행 시 Instant가 나노초 9자리의 고정 폭 문자열로 변환되어 전달된다.")
  void saveLastReadAt_FormatVerificationTest() {
    // given
    UUID convId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // 밀리초 단위까지만 있는 시각을 생성 (예: 2026-08-20T12:00:00.123Z)
    Instant readTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    SetOperations<String, Object> setOperations = mock(SetOperations.class);
    given(redisTemplate.opsForSet()).willReturn(setOperations);

    given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
        .willReturn(1L);

    // when
    repository.saveLastReadAt(convId, userId, readTime);

    // then
    ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
    verify(redisTemplate).execute(any(RedisScript.class), anyList(), argsCaptor.capture());

    // 첫 번째 ARGV 인자가 포맷팅된 시각 문자열
    String formattedTime = (String) argsCaptor.getValue();

    // 9자리 나노초 정규식 패턴과 정확히 일치하는지 확인
    assertThat(formattedTime)
        .isNotNull()
        .hasSize(30) // "yyyy-MM-ddTHH:mm:ss.SSSSSSSSSZ"는 무조건 30글자
        .endsWith("Z")
        .contains("T");
    assertThat(Instant.parse(formattedTime)).isEqualTo(readTime);

    String expectedDirtyMember = convId + ":" + userId;
    verify(setOperations).add("dm:read:dirty", expectedDirtyMember);
  }

  @Test
  @DisplayName("성공: 캐시 복구 시 setIfAbsent를 사용하며 고정 폭 포맷으로 저장한다.")
  void setCachedLastReadAt_SuccessTest() {
    // given
    UUID convId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant readTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    String valueKey = "dm:read:" + convId + ":" + userId;

    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    // when
    repository.setCachedLastReadAt(convId, userId, readTime);

    // then
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    verify(valueOperations).setIfAbsent(eq(valueKey), valueCaptor.capture(), eq(Duration.ofDays(7)));

    // 저장되는 값 역시 9자리 나노초 정규식을 만족하는지 검증
    assertThat(valueCaptor.getValue())
        .isNotNull()
        .hasSize(30) // "yyyy-MM-ddTHH:mm:ss.SSSSSSSSSZ"는 무조건 30글자
        .endsWith("Z")
        .contains("T");
  }

  @Test
  @DisplayName("성공: Redis에서 조회한 고정 폭 문자열을 정상적으로 Instant로 파싱한다.")
  void getCachedLastReadAt_SuccessTest() {
    // given
    UUID convId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String valueKey = "dm:read:" + convId + ":" + userId;

    // Redis에 저장되어 있다고 가정할 9자리 나노초 문자열
    String cachedString = "2026-08-20T12:00:00.123000000Z";

    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get(valueKey)).willReturn(cachedString);

    // when
    Optional<Instant> result = repository.getCachedLastReadAt(convId, userId);

    // then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(Instant.parse(cachedString));
  }

  @Test
  @DisplayName("성공: 처리 중인 대기열(processing)이 없을 때, 원본(dirty) 대기열을 rename하여 가져온다.")
  void getDirtyMembersWithLimit_RenameDirtySetTest() {
    // given
    SetOperations<String, Object> setOperations = mock(SetOperations.class);
    given(redisTemplate.opsForSet()).willReturn(setOperations);

    // processing 키는 없고, dirty 키는 존재하는 상태
    given(redisTemplate.hasKey("dm:read:dirty:processing")).willReturn(false);
    given(redisTemplate.hasKey("dm:read:dirty")).willReturn(true);

    given(setOperations.randomMembers("dm:read:dirty:processing", 500L))
        .willReturn(List.of("convId:userId1", "convId:userId2"));

    // when
    Set<Object> result = repository.getDirtyMembersWithLimit();

    // then
    // 1. rename 명령어가 정확히 호출되었는지 검증
    verify(redisTemplate).rename("dm:read:dirty", "dm:read:dirty:processing");
    // 2. 500개를 읽어오는 명령어가 호출되었는지 검증
    verify(setOperations).randomMembers("dm:read:dirty:processing", 500L);
    // 3. 반환된 Set에 정상적으로 매핑되었는지 검증
    assertThat(result).hasSize(2).containsExactlyInAnyOrder("convId:userId1", "convId:userId2");
  }

  @Test
  @DisplayName("성공: 이미 처리 중인 대기열(processing)이 있다면, rename을 건너뛰고 바로 조회한다.")
  void getDirtyMembersWithLimit_AlreadyProcessingTest() {
    // given
    SetOperations<String, Object> setOperations = mock(SetOperations.class);
    given(redisTemplate.opsForSet()).willReturn(setOperations);

    // 이미 누군가(다른 서버 등) processing 키를 만들어둔 상태
    given(redisTemplate.hasKey("dm:read:dirty:processing")).willReturn(true);

    given(setOperations.randomMembers("dm:read:dirty:processing", 500L))
        .willReturn(List.of("convId:userId3"));

    // when
    Set<Object> result = repository.getDirtyMembersWithLimit();

    // then
    // rename은 절대 호출되지 않아야 함 (이전 작업자가 만들어둔 걸 덮어쓰면 안 되므로)
    verify(redisTemplate, never()).rename(any(), any());
    verify(setOperations).randomMembers("dm:read:dirty:processing", 500L);
    assertThat(result).hasSize(1).contains("convId:userId3");
  }

  @Test
  @DisplayName("성공: 처리할 대기열이 아무것도 없다면 빈 Set을 반환한다.")
  void getDirtyMembersWithLimit_EmptyTest() {
    // given
    // 둘 다 존재하지 않는 상태
    given(redisTemplate.hasKey("dm:read:dirty:processing")).willReturn(false);
    given(redisTemplate.hasKey("dm:read:dirty")).willReturn(false);

    // when
    Set<Object> result = repository.getDirtyMembersWithLimit();

    // then
    verify(redisTemplate, never()).rename(any(), any());
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("성공: 처리가 완료된 멤버들을 processing 대기열에서 일괄 삭제한다.")
  void removeProcessedMembers_SuccessTest() {
    // given
    SetOperations<String, Object> setOperations = mock(SetOperations.class);
    given(redisTemplate.opsForSet()).willReturn(setOperations);

    Set<Object> processedMembers = new LinkedHashSet<>(List.of("convId:userId1", "convId:userId2"));

    // when
    repository.removeProcessedMembers(processedMembers);

    // then
    verify(setOperations).remove("dm:read:dirty:processing", "convId:userId1", "convId:userId2");
  }

  @Test
  @DisplayName("예외 치유: Redis에 잘못된 날짜 포맷이 들어있다면 파싱 에러를 뱉고 해당 키를 삭제한다.")
  void getCachedLastReadAt_ParseException_AutoHealTest() {
    // given
    UUID convId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String valueKey = "dm:read:" + convId + ":" + userId;

    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    // 쓰레기값이 들어있다고 가정
    given(valueOperations.get(valueKey)).willReturn("invalid-date-format-trash-data");

    // when
    Optional<Instant> result = repository.getCachedLastReadAt(convId, userId);

    // then
    // DB 조회로 Fallback 되게 만듦
    assertThat(result).isEmpty();
    // 잘못된 키는 자연 치유 로직에 의해 즉각 삭제되어야 함
    verify(redisTemplate).delete(valueKey);
  }
}