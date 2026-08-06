package com.codeit.mople.domain.watchingsession.service;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.exception.ContentErrorCode;
import com.codeit.mople.domain.content.exception.ContentException;
import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionChange;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionContentDto;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionResponse;
import com.codeit.mople.global.dto.UserSummary;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final ContentRepository contentRepository;
  private final UserRepository userRepository; // UserRepository 주입
  private final RedisTemplate<String, Object> redisTemplate;
  private final SimpMessagingTemplate messagingTemplate;

  private static final String USER_WATCHING_KEY_PREFIX = "user:watching:";
  private static final String CONTENT_WATCHERS_KEY_PREFIX = "content:watchers:";

  @Transactional(readOnly = true)
  public CursorResponseWatchingSessionDto getWatchingSessions(
      UUID contentId, String watcherNameLike, String cursor, UUID idAfter,
      int limit, String sortDirection, String sortBy) {
    log.debug("시청 세션 목록 조회 시작 - contentId: {}", contentId);

    //콘텐츠 존재 여부 예외 처리
    Content content = contentRepository.findById(contentId)
        .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));

    //limit 검증
    if (limit <= 0 || limit > 100) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("limit", limit));
    }

    //정렬 기준 및 정렬 방향 정규화
    if (sortBy == null) {
      sortBy = "id";
    }
    if (sortDirection == null) {
      sortDirection = "ASCENDING";
    }

    //정렬 기준 및 정렬 방향 검증
    if (!"ASCENDING".equalsIgnoreCase(sortDirection) &&
        !"DESCENDING".equalsIgnoreCase(sortDirection)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("sortDirection", sortDirection));
    }

    //커서 쌍 검증
    if ((cursor == null) != (idAfter == null)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST,
          Map.of("cursor", String.valueOf(cursor), "idAfter", String.valueOf(idAfter)));
    }

    //Redis에서 현재 실시간으로 시청 중인 유저 ID 전체 목록 조회
    Set<UUID> watcherIds = getWatcherIds(contentId);

    //Redis Set은 순서가 없으므로 정렬 방향에 맞춰 리스트로 변환 및 정렬(메모리 정렬)
    List<String> watcherIdList = watcherIds.stream()
        .map(UUID::toString)
        .sorted("DESCENDING".equalsIgnoreCase(sortDirection)
            ? Collections.reverseOrder()
            : String::compareTo)
        .toList();

    //전체 데이터 수 카운트
    long totalCount = watcherIdList.size();

    //커서 위치 탐색(idAfter 기준)
    int startIndex = 0;
    if (idAfter != null) {
      int foundIndex = watcherIdList.indexOf(idAfter.toString());
      if (foundIndex != -1) {
        startIndex = foundIndex + 1; //커서 다음 항목부터 시작
      }
    }

    //hasNext 판단 및 limit 사이즈만큼 자르기(Slicing)
    int endIndex = Math.min(startIndex + limit + 1, watcherIdList.size());
    List<String> pagedIdStrings = startIndex < watcherIdList.size()
        ? watcherIdList.subList(startIndex, endIndex)
        : Collections.emptyList();

    boolean hasNext = pagedIdStrings.size() > limit;
    List<String> resultIds = hasNext ? pagedIdStrings.subList(0, limit) : pagedIdStrings;

    //페이징된 target UUID 리스트 추출
    List<UUID> targetUserIds = resultIds.stream().map(UUID::fromString).toList();

    //DB에서 페이징 대상 유저들을 한 번에 조회하여 Map으로 캐싱 (N+1 방지)
    Map<UUID, User> userMap = userRepository.findAllById(targetUserIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

    //콘텐츠 정보를 담을 DTO 생성
    WatchingSessionContentDto contentDto = new WatchingSessionContentDto(
        content.getId(), content.getType().name(), content.getTitle(),
        content.getDescription(), content.getThumbnailUrl(), content.getTags(),
        content.getAverageRating(), content.getReviewCount()
    );

    //Redis 데이터 -> response dto 매핑
    List<WatchingSessionResponse> responses = resultIds.stream().map(idStr -> {
      UUID uId = UUID.fromString(idStr);
      User user = userMap.get(uId);

      //유저가 DB에 없을 경우를 대비한 Null-safe 방어 로직
      String name = user != null ? user.getName() : "알 수 없는 유저";
      String profileImageUrl = user != null ? user.getProfileImageUrl() : null;

      UserSummary userSummary = new UserSummary(uId, name, profileImageUrl);

      return new WatchingSessionResponse(
          UUID.randomUUID(), //실시간 세션 식별용 임시 ID
          Instant.now(),
          userSummary,
          contentDto
      );
    }).toList();

    //다음 커서 값 추출
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !resultIds.isEmpty()) {
      String lastId = resultIds.get(resultIds.size() - 1);
      nextCursor = lastId;
      nextIdAfter = UUID.fromString(lastId);
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
    Long watcherCount = redisTemplate.opsForSet().size(contentKey);

    //웹소켓으로 입장 이벤트 브로드캐스팅
    WatchingSessionChange changeEvent = new WatchingSessionChange(
        contentId.toString(),
        userId,
        "ENTER",
        watcherCount
    );
    messagingTemplate.convertAndSend(
        "/sub/contents/" + contentId.toString() + "/watch", changeEvent);

    return watcherCount;
  }

  //유저가 콘텐츠 시청을 종료(퇴장)할 때 Redis에서 세션을 제거
  public Long leaveSession(UUID userId, UUID contentId) {
    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();

    //해당 유저가 시청 중이라는 상태 삭제
    redisTemplate.delete(userKey);

    //콘텐츠의 실시간 시청자 목록에서 해당 유저 제거
    redisTemplate.opsForSet().remove(contentKey, userId.toString());

    //퇴장 후 남은 총 시청자 수 반환(키가 만료되거나 없으면 0반환)
    Long remainingCount = redisTemplate.opsForSet().size(contentKey);
    Long watcherCount = remainingCount != null ? remainingCount : 0L;

    //웹소켓으로 퇴장 이벤트 브로드캐스팅
    WatchingSessionChange changeEvent = new WatchingSessionChange(
        contentId.toString(),
        userId,
        "LEAVE",
        watcherCount
    );
    messagingTemplate.convertAndSend(
        "/sub/contents/" + contentId.toString() + "/watch", changeEvent);

    return watcherCount;
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