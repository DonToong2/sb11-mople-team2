package com.codeit.mople.domain.watchingsession.service;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.exception.ContentErrorCode;
import com.codeit.mople.domain.content.exception.ContentException;
import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.domain.watchingsession.dto.ContentChatDto;
import com.codeit.mople.domain.watchingsession.dto.ContentChatSendRequest;
import com.codeit.mople.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mople.domain.watchingsession.dto.WatcherUserDto;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionChange;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionContentDto;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionDetailDto;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionEvent;
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionResponse;
import com.codeit.mople.global.dto.UserSummary;
import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Generated
public class WatchingSessionService {

  private final ContentRepository contentRepository;
  private final UserRepository userRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final SimpMessagingTemplate messagingTemplate;
  private final ApplicationEventPublisher eventPublisher;

  private static final String USER_WATCHING_KEY_PREFIX = "user:watching:";
  private static final String CONTENT_WATCHERS_KEY_PREFIX = "content:watchers:";
  private static final String USER_SESSION_ID_KEY_PREFIX = "user:session:id:"; //유저별 고유 세션 ID 보관용 Redis 키 프리픽스

  //내부 DTO: Redis ZSet에서 꺼낸 유저 ID와 실데 입장 시각
  private record WatcherData(String userId, Instant joinedAt) {}

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

    //정렬 기준 검증(null이거나 빈 값이면 기본값 id 부여, 지원하지 않는 값이면 400 예외 발생)
    if (sortBy == null || sortBy.isBlank()) {
      sortBy = "createdAt";
    } else if (!"createdAt".equalsIgnoreCase(sortBy)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("sortBy", sortBy));
    }

    //정렬 방향 검증(null이거나 빈 값이면 기본값 ASCENDING 부여, 지원하지 않는 값이면 400 예외 발생)
    if (sortDirection == null || sortDirection.isBlank()) {
      sortDirection = "ASCENDING";
    } else if (!"ASCENDING".equalsIgnoreCase(sortDirection) && !"DESCENDING".equalsIgnoreCase(sortDirection)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("sortDirection", sortDirection));
    }

    //커서 쌍 검증
    if ((cursor == null) != (idAfter == null)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST,
          Map.of("cursor", String.valueOf(cursor), "idAfter", String.valueOf(idAfter)));
    }

    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();
    boolean isDesc = "DESCENDING".equalsIgnoreCase(sortDirection);

    //Redis ZSet에서 점수(입장 시각)와 함께 정렬된 상태로 조회
    Set<TypedTuple<Object>> tuples = isDesc
        ? redisTemplate.opsForZSet().reverseRangeWithScores(contentKey, 0, -1)
        : redisTemplate.opsForZSet().rangeWithScores(contentKey, 0, -1);

    if (tuples == null) {
      tuples = Collections.emptySet();
    }

    List<WatcherData> watcherDataList = tuples.stream()
        .map(t -> new WatcherData((String) t.getValue(), Instant.ofEpochMilli(t.getScore().longValue())))
        .toList();

    //이름 검색 조건 필터링
    if (watcherNameLike != null && !watcherNameLike.trim().isEmpty()) {
      Set<UUID> idsToSearch = watcherDataList.stream()
          .map(w -> UUID.fromString(w.userId()))
          .collect(Collectors.toSet());

      Set<UUID> matchingIds = userRepository.findAllById(idsToSearch).stream()
          .filter(user -> user.getName() != null && user.getName().contains(watcherNameLike.trim()))
          .map(User::getId)
          .collect(Collectors.toSet());

      watcherDataList = watcherDataList.stream()
          .filter(w -> matchingIds.contains(UUID.fromString(w.userId())))
          .toList();
    }

    long totalCount = watcherDataList.size();
    int startIndex = 0;

    //커서 위치 탐색(idAfter 기준)
    if (idAfter != null && cursor != null) {
      String targetId = idAfter.toString();
      int foundIndex = -1;
      for (int i = 0; i < watcherDataList.size(); i++) {
        if (watcherDataList.get(i).userId().equals(targetId)) {
          foundIndex = i;
          break;
        }
      }

      if (foundIndex != -1) {
        startIndex = foundIndex + 1;
      } else {
        try {
          long targetTime = Long.parseLong(cursor);
          for (int i = 0; i < watcherDataList.size(); i++) {
            long wTime = watcherDataList.get(i).joinedAt().toEpochMilli();
            if ((isDesc && wTime <= targetTime) || (!isDesc && wTime >= targetTime)) {
              startIndex = i;
              break;
            }
            if (i == watcherDataList.size() - 1) {
              startIndex = watcherDataList.size();
            }
          }
        } catch (NumberFormatException e) {
          startIndex = 0;
        }
      }
    }

    int endIndex = Math.min(startIndex + limit + 1, watcherDataList.size());
    List<WatcherData> pagedData = startIndex < watcherDataList.size()
        ? watcherDataList.subList(startIndex, endIndex)
        : Collections.emptyList();

    boolean hasNext = pagedData.size() > limit;
    List<WatcherData> resultData = hasNext ? pagedData.subList(0, limit) : pagedData;

    List<UUID> targetUserIds = resultData.stream()
        .map(w -> UUID.fromString(w.userId()))
        .toList();

    Map<UUID, User> userMap = userRepository.findAllById(targetUserIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

    WatchingSessionContentDto contentDto = new WatchingSessionContentDto(
        content.getId(), content.getType().name(), content.getTitle(),
        content.getDescription(), content.getThumbnailUrl(), content.getTags(),
        content.calculateAverageRating(), content.getReviewCount()
    );

    List<WatchingSessionResponse> responses = resultData.stream().map(w -> {
      UUID uId = UUID.fromString(w.userId());
      User user = userMap.get(uId);

      String name = user != null ? user.getName() : "알 수 없는 유저";
      String profileImageUrl = user != null ? user.getProfileImageUrl() : null;

      UserSummary userSummary = new UserSummary(uId, name, profileImageUrl);

      //ZSet에 저장되어 있던 실제 입장 시각(w.joinedAt()) 사용
      return new WatchingSessionResponse(
          UUID.randomUUID(),
          w.joinedAt(),
          userSummary,
          contentDto
      );
    }).toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !resultData.isEmpty()) {
      WatcherData lastItem = resultData.get(resultData.size() - 1);
      nextCursor = String.valueOf(lastItem.joinedAt().toEpochMilli());
      nextIdAfter = UUID.fromString(lastItem.userId());
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

  //유저가 콘텐츠 시청을 시작(입장)할 때 실시간 세션을 Redis에 기록하고 DB 갱신
  @Transactional
  public Long enterSession(UUID userId, UUID contentId) {
    //DB 검증을 최상단 위치, 콘텐츠가 없으면 Redis 조작 전에 롤백(예외)시킴
    Content currentContent = contentRepository.findById(contentId)
        .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));

    UserSummary userSummary = userRepository.findById(userId)
        .map(user -> new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl()))
        .orElseGet(() -> new UserSummary(userId, "알 수 없는 유저", null));

    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();
    String sessionIdKey = USER_SESSION_ID_KEY_PREFIX + userId.toString(); //유저별 세션 ID 관리 키

    //Redis 세션 전환을 재시도 가능한 트랜잭션(SessionCallback)으로 원자적 처리
    String previousContentId = null;
    boolean txSuccess = false;
    int maxRetries = 5;

    //이미 발급된 세션 UUID가 존재하면 재사용하고, 없으면 새로 생성하여 입장/퇴장 시 ID 불일치 방지
    String existingSessionIdStr = (String) redisTemplate.opsForValue().get(sessionIdKey);
    final UUID sessionUuid = (existingSessionIdStr != null) ? UUID.fromString(existingSessionIdStr) : UUID.randomUUID();

    for (int i = 0; i < maxRetries; i++) {
      long joinTime = Instant.now().toEpochMilli();
      Object result = redisTemplate.execute(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
          operations.watch(userKey);
          String prevId = (String) operations.opsForValue().get(userKey);

          operations.multi();
          if (prevId != null && !prevId.equals(contentId.toString())) {
            operations.opsForZSet().remove(CONTENT_WATCHERS_KEY_PREFIX + prevId, userId.toString());
          }
          operations.opsForValue().set(userKey, contentId.toString());
          operations.opsForValue().set(sessionIdKey, sessionUuid.toString());
          operations.opsForZSet().add(contentKey, userId.toString(), joinTime);

          List<Object> execResult = operations.exec();
          if (execResult == null || execResult.isEmpty()) {
            return null;
          }
          return prevId == null ? "NULL_PREV" : prevId;
        }
      });

      if (result != null) {
        txSuccess = true;
        previousContentId = "NULL_PREV".equals(result) ? null : (String) result;
        break;
      }
    }

    if (!txSuccess) {
      log.error("입장 Redis 트랜잭션 실패 (최대 재시도 초과) - userId: {}", userId);
      throw new RuntimeException("일시적인 오류가 발생했습니다. 다시 시도해주세요.");
    }

    WatcherUserDto watcherUser = new WatcherUserDto(
        userSummary.userId(),
        userSummary.userId(),
        userSummary.name(),
        userSummary.profileImageUrl()
    );

    WatchingSessionContentDto contentDto = new WatchingSessionContentDto(
        currentContent.getId(), currentContent.getType().name(), currentContent.getTitle(),
        currentContent.getDescription(), currentContent.getThumbnailUrl(), currentContent.getTags(),
        currentContent.calculateAverageRating(), currentContent.getReviewCount()
    );

    //유저가 다른 콘텐츠를 보고 있었다면 이전 기록 삭제(방 이동 고려)
    if (previousContentId != null && !previousContentId.equals(contentId.toString())) {
      String prevContentKey = CONTENT_WATCHERS_KEY_PREFIX + previousContentId;

      Long prevCount = redisTemplate.opsForZSet().zCard(prevContentKey);
      int prevWatcherCountInt = prevCount != null ? prevCount.intValue() : 0;

      Content prevContentEntity = contentRepository.findById(UUID.fromString(previousContentId)).orElse(null);
      if (prevContentEntity != null) {
        prevContentEntity.updateWatcherCount((long) prevWatcherCountInt);
      }

      WatchingSessionContentDto prevContentDto = prevContentEntity != null ? new WatchingSessionContentDto(
          prevContentEntity.getId(), prevContentEntity.getType().name(), prevContentEntity.getTitle(),
          prevContentEntity.getDescription(), prevContentEntity.getThumbnailUrl(), prevContentEntity.getTags(),
          prevContentEntity.calculateAverageRating(), prevContentEntity.getReviewCount()
      ) : contentDto;

      //방 이동 시에도 동일한 sessionUuid를 사용하여 일관성 유지
      WatchingSessionDetailDto prevDetail = new WatchingSessionDetailDto(
          sessionUuid,
          Instant.now(),
          watcherUser,
          prevContentDto
      );

      //이전 방 퇴장 이벤트 생성
      WatchingSessionChange prevChangeEvent = new WatchingSessionChange(
          "LEAVE",
          prevDetail,
          prevWatcherCountInt
      );

      eventPublisher.publishEvent(new WatchingSessionEvent(UUID.fromString(previousContentId), prevChangeEvent));
    }

    //현재 해당 콘텐츠를 보고 있는 총 시청자 수 반환
    Long watcherCount = redisTemplate.opsForZSet().zCard(contentKey);
    int currentWatcherCountInt = watcherCount != null ? watcherCount.intValue() : 0;

    currentContent.updateWatcherCount((long) currentWatcherCountInt);

    // 현재 방 입장 이벤트 생성
    WatchingSessionDetailDto sessionDetail = new WatchingSessionDetailDto(
        sessionUuid,
        Instant.now(),
        watcherUser,
        contentDto
    );

    WatchingSessionChange changeEvent = new WatchingSessionChange(
        "JOIN",
        sessionDetail,
        currentWatcherCountInt
    );

    eventPublisher.publishEvent(new WatchingSessionEvent(contentId, changeEvent));

    return watcherCount;
  }

  //유저가 콘텐츠 시청을 종료(퇴장)할 때 Redis에서 세션을 제거하고 DB 갱신
  @Transactional
  public Long leaveSession(UUID userId, UUID contentId) {
    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();
    String sessionIdKey = USER_SESSION_ID_KEY_PREFIX + userId.toString();

    //삭제하기 직전, 입장 시 발급되어 저장되었던 고유 세션 ID를 조회해 옴(프론트엔드 매칭용)
    String sessionIdStr = (String) redisTemplate.opsForValue().get(sessionIdKey);
    UUID sessionUuid = (sessionIdStr != null) ? UUID.fromString(sessionIdStr) : UUID.randomUUID();

    boolean txSuccess = false;
    boolean wasWatching = false;
    int maxRetries = 5;

    for (int i = 0; i < maxRetries; i++) {
      Object result = redisTemplate.execute(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
          operations.watch(userKey);
          String currentWatchingId = (String) operations.opsForValue().get(userKey);

          if (currentWatchingId == null || !currentWatchingId.equals(contentId.toString())) {
            operations.unwatch();
            return "NOT_WATCHING";
          }

          operations.multi();
          operations.delete(userKey);
          operations.delete(sessionIdKey);
          operations.opsForZSet().remove(contentKey, userId.toString());

          List<Object> execResult = operations.exec();
          if (execResult == null || execResult.isEmpty()) {
            return null; //충돌 발생, 재시도
          }
          return "SUCCESS";
        }
      });

      if (result != null) {
        txSuccess = true;
        wasWatching = !"NOT_WATCHING".equals(result);
        break;
      }
    }

    if (!txSuccess) {
      log.error("퇴장 Redis 트랜잭션 실패 (최대 재시도 초과) - userId: {}", userId);
      throw new RuntimeException("일시적인 오류가 발생했습니다. 다시 시도해주세요.");
    }

    if (!wasWatching) {
      log.warn("퇴장 요청 무시: 유저가 해당 콘텐츠를 시청 중이지 않음. userId: {}, contentId: {}", userId, contentId);
      Long currentCount = redisTemplate.opsForZSet().zCard(contentKey);
      return currentCount != null ? currentCount : 0L;
    }

    Long remainingCount = redisTemplate.opsForZSet().zCard(contentKey);
    Long watcherCount = remainingCount != null ? remainingCount : 0L;
    int watcherCountInt = watcherCount.intValue();

    Content content = contentRepository.findById(contentId).orElse(null);
    if (content != null) {
      content.updateWatcherCount(watcherCount);
    }

    UserSummary userSummary = userRepository.findById(userId)
        .map(user -> new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl()))
        .orElseGet(() -> new UserSummary(userId, "알 수 없는 유저", null));

    WatcherUserDto watcherUser = new WatcherUserDto(
        userSummary.userId(),
        userSummary.userId(),
        userSummary.name(),
        userSummary.profileImageUrl()
    );

    WatchingSessionContentDto contentDto = content != null ? new WatchingSessionContentDto(
        content.getId(), content.getType().name(), content.getTitle(),
        content.getDescription(), content.getThumbnailUrl(), content.getTags(),
        content.calculateAverageRating(), content.getReviewCount()
    ) : null;

    //퇴장 이벤트 생성
    WatchingSessionDetailDto sessionDetail = new WatchingSessionDetailDto(
        sessionUuid,
        Instant.now(),
        watcherUser,
        contentDto
    );

    WatchingSessionChange changeEvent = new WatchingSessionChange(
        "LEAVE",
        sessionDetail,
        watcherCountInt
    );

    eventPublisher.publishEvent(new WatchingSessionEvent(contentId, changeEvent));

    return watcherCount;
  }

  //특정 유저가 현재 시청 중인 콘텐츠의 ID를 조회
  //시청 세션이 없을 경우 404 예외 던짐
  public UUID getWatchingContentId(UUID userId) {
    //유저 존재 여부 확인
    userRepository.findById(userId)
        .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("userId", userId)));

    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentIdStr = (String) redisTemplate.opsForValue().get(userKey);

    if (contentIdStr == null) {
      throw new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("watcherId", userId));
    }

    return UUID.fromString(contentIdStr);
  }

  //실시간 채팅 메시지 처리 및 브로드캐스팅
  @Transactional(readOnly = true)
  public void broadcastChatMessage(String contentIdStr, ContentChatSendRequest request, Principal principal) {
    log.debug("웹소켓 채팅 요청 수신 - contentId: {}, request: {}", contentIdStr, request);

    //인증 객체 검증
    if (principal == null) {
      log.warn("채팅 전송 실패: Principal(인증 객체)이 null입니다. JwtChannelInterceptor 확인 요망.");
      return;
    }

    //메시지 본문 방어
    if (request == null || request.message() == null || request.message().isBlank()) {
      log.warn("채팅 메시지가 비어있어 브로드캐스팅을 취소합니다. request: {}", request);
      return;
    }

    //안전한 타입 검사 및 변환
    if (principal instanceof UsernamePasswordAuthenticationToken authentication &&
        authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

      UUID senderId = userDetails.getUserId();
      UUID contentId;

      try {
        contentId = UUID.fromString(contentIdStr);
      } catch (IllegalArgumentException e) {
        log.warn("채팅 전송 실패: 올바르지 않은 UUID 형식입니다. contentIdStr: {}", contentIdStr);
        return;
      }

      //DB에서 유저 조회하여 프론트엔드에 전달할 UserSummary 객체 생성(프로필 이미지 포함)
      UserSummary userSummary = userRepository.findById(senderId)
          .map(user -> new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl()))
          .orElseGet(() -> new UserSummary(senderId, "알 수 없는 유저", null));

      Instant now = Instant.now();

      //브로드캐스팅할 DTO 생성
      ContentChatDto response = new ContentChatDto(
          contentId.toString(),
          userSummary.userId(),
          userSummary.name(),
          userSummary,
          userSummary,
          request.message(),
          request.message(),
          now,
          now
      );

      //브로드캐스팅
      messagingTemplate.convertAndSend("/sub/contents/" + contentId.toString() + "/chat", response);

      log.info("채팅 메시지 브로드캐스팅 완료 - contentId: {}, senderName: {}", contentId, userSummary.name());
    }
  }
}