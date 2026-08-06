package com.codeit.mople.domain.watchingsession.service;

import com.codeit.mople.domain.content.exception.ContentErrorCode;
import com.codeit.mople.domain.content.exception.ContentException;
import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionContentDto;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionResponse;
import com.codeit.mople.domain.watchingsession.entity.WatchingSession;
import com.codeit.mople.domain.watchingsession.repository.WatchingSessionQueryRepository;
import com.codeit.mople.global.dto.UserSummary;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionQueryRepository watchingSessionQueryRepository;
  private final ContentRepository contentRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private static final String USER_WATCHING_KEY_PREFIX = "user:watching:";
  private static final String CONTENT_WATCHERS_KEY_PREFIX = "content:watchers:";

  @Transactional(readOnly = true)
  public CursorResponseWatchingSessionDto getWatchingSessions(
      UUID contentId, String watcherNameLike, String cursor, UUID idAfter,
      int limit, String sortDirection, String sortBy) {
    log.debug("시청 세션 목록 조회 시작 - contentId: {}", contentId);

    //콘텐츠 존재 여부 예외 처리
    if (!contentRepository.existsById(contentId)) {
      throw new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId));
    }

    //limit 검증
    if (limit <= 0 || limit > 100) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("limit", limit));
    }

    //정렬 기준 및 정렬 방향 정규화
    if (sortBy == null) {
      sortBy = "createdAt";
    }
    if (sortDirection == null) {
      sortDirection = "ASCENDING";
    }

    //정렬 기준 및 정렬 방향 검증
    if (!"createdAt".equalsIgnoreCase(sortBy)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("sortBy", sortBy));
    }
    if (!"ASCENDING".equalsIgnoreCase(sortDirection) &&
        !"DESCENDING".equalsIgnoreCase(sortDirection)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("sortDirection", sortDirection));
    }

    //커서 쌍 검증
    if ((cursor == null) != (idAfter == null)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST,
          Map.of("cursor", String.valueOf(cursor), "idAfter", String.valueOf(idAfter)));
    }

    //커서 날짜 포맷 검증(500에러를 400 Bad Request로 변환)
    if (cursor != null) {
      try {
        Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("cursor", cursor));
      }
    }

    //QueryDSL 레포지토리 호출(limit + 1개 조회)
    List<WatchingSession> sessions = watchingSessionQueryRepository.findSessionByCursor(
        contentId, watcherNameLike, cursor, idAfter, limit, sortBy, sortDirection);

    //전체 데이터 수 카운트
    long totalCount = watchingSessionQueryRepository.countSessions(contentId, watcherNameLike);

    //hasNext 판단 및 limit 사이즈만큼 자르기
    boolean hasNext = sessions.size() > limit;
    List<WatchingSession> pageSessions = hasNext ? sessions.subList(0, limit) : sessions;

    //entity -> response dto 매핑
    List<WatchingSessionResponse> responses = pageSessions.stream()
        .map(session -> new WatchingSessionResponse(
            session.getId(),
            session.getCreatedAt(),
            new UserSummary(
                session.getUser().getId(),
                session.getUser().getName(),
                session.getUser().getProfileImageUrl()
            ),
            new WatchingSessionContentDto(
                session.getContent().getId(),
                session.getContent().getType().name(),
                session.getContent().getTitle(),
                session.getContent().getDescription(),
                session.getContent().getThumbnailUrl(),
                session.getContent().getTags(),
                session.getContent().getAverageRating(),
                session.getContent().getReviewCount()
            )
        )).toList();

    //다음 커서 값 추출
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !pageSessions.isEmpty()) {
      WatchingSession lastItem = pageSessions.get(pageSessions.size() - 1);
      nextCursor = lastItem.getCreatedAt() != null ? lastItem.getCreatedAt().toString() : null;
      nextIdAfter = lastItem.getId();
    }

    //최종 CursorResponse DTO 반환
    return new CursorResponseWatchingSessionDto(
        responses,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        sortBy,
        sortDirection
    );
  }

  //유저가 콘텐츠 시청을 시작(입장)할 때 실시간 세션을 Redis에 기록
  public Long enterSession(UUID userId, UUID contentId) {
    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();

    //유저가 다른 콘텐츠를 보고 있었다면 이전 기록 삭제(방 이동 고려)
    String previousContentId = (String) redisTemplate.opsForValue().get(userKey);
    if (previousContentId != null && !previousContentId.equals(contentId.toString())) {
      String prevContentKey = CONTENT_WATCHERS_KEY_PREFIX + previousContentId;
      redisTemplate.opsForSet().remove(prevContentKey, userId.toString());
    }

    //유저별 현재 시청 중인 콘텐츠 업데이트(String 자료구조)
    redisTemplate.opsForValue().set(userKey, contentId.toString());

    //콘텐츠별 시청자 목록에 추가(Set 자료구조 - 중복 방지)
    redisTemplate.opsForSet().add(contentKey, userId.toString());

    //현재 해당 콘텐츠를 보고 있는 총 시청자 수 반환
    return redisTemplate.opsForSet().size(contentKey);
  }

  //유저가 콘텐츠 시청을 종료(퇴장)할 때 Redis에서 세션을 제거
  public Long leaveSession(UUID userId, UUID contentId) {
    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();

    //해당 유저가 시청 중이라는 상태 삭제
    redisTemplate.delete(userKey);

    //콘텐츠의 실시간 시청자 목록에서 해당 유저 제거
    redisTemplate.opsForSet().remove(contentKey, userId.toString());

    //퇴장 후 남은 총 시청자 수 반환(키가 만료되거나 없으면 0반환
    Long remainingCount = redisTemplate.opsForSet().size(contentKey);
    return remainingCount != null ? remainingCount : 0L;
  }

  //특정 유저가 현재 시청 중인 콘텐츠의 ID를 조회
  public UUID getWatchingContentId(UUID userId) {
    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentIdStr = (String) redisTemplate.opsForValue().get(userKey);

    return contentIdStr != null ? UUID.fromString(contentIdStr) : null;
  }

  //특정 콘텐츠를 현재 실시간으로 시청 중인 유저 ID 목록을 조회
  public Set<UUID> getWatcherIds(UUID contentId) {
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();
    Set<Object> members = redisTemplate.opsForSet().members(contentKey);

    if (members == null || members.isEmpty()) {
      return Collections.emptySet();
    }

    return members.stream()
        .map(member -> UUID.fromString((String) member))
        .collect(Collectors.toSet());
  }
}
