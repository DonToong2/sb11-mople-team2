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
import com.codeit.mople.domain.watchingsession.dto.WatchingSessionResponse;
import com.codeit.mople.global.dto.UserSummary;
import com.codeit.mople.global.sse.service.SseService;
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
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
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
  private final SseService sseService;

  private static final String USER_WATCHING_KEY_PREFIX = "user:watching:";
  private static final String CONTENT_WATCHERS_KEY_PREFIX = "content:watchers:";
  private static final String USER_SESSION_ID_KEY_PREFIX = "user:session:id:"; //유저별 고유 세션 ID 보관용 Redis 키 프리픽스

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

    if (sortBy == null || sortBy.isBlank() || !"id".equalsIgnoreCase(sortBy)) {
      sortBy = "id";
    }

    if (sortDirection == null || sortDirection.isBlank() ||
        (!"ASCENDING".equalsIgnoreCase(sortDirection) && !"DESCENDING".equalsIgnoreCase(sortDirection))) {
      sortDirection = "ASCENDING";
    }

    //커서 쌍 검증
    if ((cursor == null) != (idAfter == null)) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST,
          Map.of("cursor", String.valueOf(cursor), "idAfter", String.valueOf(idAfter)));
    }

    //Redis에서 현재 실시간으로 시청 중인 유저 ID 전체 목록 조회
    Set<UUID> watcherIds = getWatcherIds(contentId);

    //이름 검색 조건이 있을 경우 메모리 필터링
    if (watcherNameLike != null && !watcherNameLike.trim().isEmpty()) {
      List<User> matchingUsers = userRepository.findAllById(watcherIds).stream()
          .filter(user -> user.getName() != null && user.getName().contains(watcherNameLike.trim()))
          .toList();

      //필터링된 유저들의 ID로 watcherIds 교체
      watcherIds = matchingUsers.stream()
          .map(User::getId)
          .collect(Collectors.toSet());
    }

    //Redis Set은 순서가 없으므로 정렬 방향에 맞춰 리스트로 변환 및 정렬(메모리 정렬)
    boolean isDesc = "DESCENDING".equalsIgnoreCase(sortDirection);
    List<String> watcherIdList = watcherIds.stream()
        .map(UUID::toString)
        .sorted(isDesc ? Collections.reverseOrder() : String::compareTo)
        .toList();

    //전체 데이터 수 카운트
    long totalCount = watcherIdList.size();

    //커서 위치 탐색(idAfter 기준)
    //커서 유저가 퇴장해서 idAfter를 찾아내지 못한 경우(-1) 정렬 위치 계산
    int startIndex = 0;
    if (idAfter != null) {
      String targetId = idAfter.toString();
      int foundIndex = watcherIdList.indexOf(targetId);
      if (foundIndex != -1) {
        startIndex = foundIndex + 1; //커서 다음 항목부터 시작
      } else {
        // 유저가 퇴장하여 커서 ID를 찾지 못한 경우 정렬 위치 보정
        for (int i = 0; i < watcherIdList.size(); i++) {
          int cmp = watcherIdList.get(i).compareTo(targetId);
          if ((isDesc && cmp < 0) || (!isDesc && cmp > 0)) {
            startIndex = i;
            break;
          }
          if (i == watcherIdList.size() - 1) {
            startIndex = watcherIdList.size();
          }
        }
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
        content.calculateAverageRating(), content.getReviewCount()
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

  //유저가 콘텐츠 시청을 시작(입장)할 때 실시간 세션을 Redis에 기록하고 DB 갱신
  @Transactional
  public Long enterSession(UUID userId, UUID contentId) {
    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();
    String sessionIdKey = USER_SESSION_ID_KEY_PREFIX + userId.toString(); //유저별 세션 ID 관리 키

    //Redis 세션 전환을 재시도 가능한 트랜잭션(SessionCallback)으로 원자적 처리
    String previousContentId = null;
    boolean txSuccess = false;
    int maxRetries = 5;

    // ⭐ 핵심 수정: 이미 발급된 세션 UUID가 존재하면 재사용하고, 없으면 새로 생성하여 입장/퇴장 시 ID 불일치 방지
    String existingSessionIdStr = (String) redisTemplate.opsForValue().get(sessionIdKey);
    final UUID sessionUuid = (existingSessionIdStr != null) ? UUID.fromString(existingSessionIdStr) : UUID.randomUUID();

    for (int i = 0; i < maxRetries; i++) {
      Object result = redisTemplate.execute(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
          operations.watch(userKey);
          String prevId = (String) operations.opsForValue().get(userKey);

          operations.multi();
          if (prevId != null && !prevId.equals(contentId.toString())) {
            operations.opsForSet().remove(CONTENT_WATCHERS_KEY_PREFIX + prevId, userId.toString());
          }
          operations.opsForValue().set(userKey, contentId.toString());
          operations.opsForValue().set(sessionIdKey, sessionUuid.toString()); //유저별 고유 세션 ID를 Redis에 저장
          operations.opsForSet().add(contentKey, userId.toString());

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

    UserSummary userSummary = userRepository.findById(userId)
        .map(user -> new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl()))
        .orElseGet(() -> new UserSummary(userId, "알 수 없는 유저", null));

    WatcherUserDto watcherUser = new WatcherUserDto(
        userSummary.userId(),
        userSummary.userId(),
        userSummary.name(),
        userSummary.profileImageUrl()
    );

    Content currentContent = contentRepository.findById(contentId)
        .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));

    WatchingSessionContentDto contentDto = new WatchingSessionContentDto(
        currentContent.getId(), currentContent.getType().name(), currentContent.getTitle(),
        currentContent.getDescription(), currentContent.getThumbnailUrl(), currentContent.getTags(),
        currentContent.calculateAverageRating(), currentContent.getReviewCount()
    );

    //유저가 다른 콘텐츠를 보고 있었다면 이전 기록 삭제(방 이동 고려)
    if (previousContentId != null && !previousContentId.equals(contentId.toString())) {
      String prevContentKey = CONTENT_WATCHERS_KEY_PREFIX + previousContentId;

      Long prevCount = redisTemplate.opsForSet().size(prevContentKey);
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

      // 이전 방 퇴장 이벤트 생성 (프로토타입 규격: type="LEAVE")
      WatchingSessionChange prevChangeEvent = new WatchingSessionChange(
          "LEAVE",
          prevDetail,
          prevWatcherCountInt
      );
      messagingTemplate.convertAndSend("/sub/contents/" + previousContentId + "/watch", prevChangeEvent);

      // SSE 발송
      Set<UUID> prevWatcherIds = getWatcherIds(UUID.fromString(previousContentId));
      for (UUID wId : prevWatcherIds) {
        sseService.send(wId, "watch", prevChangeEvent);
      }
    }

    //현재 해당 콘텐츠를 보고 있는 총 시청자 수 반환
    Long watcherCount = redisTemplate.opsForSet().size(contentKey);
    int currentWatcherCountInt = watcherCount != null ? watcherCount.intValue() : 0;

    currentContent.updateWatcherCount((long) currentWatcherCountInt);

    // 현재 방 입장 이벤트 생성 (프로토타입 규격: type="JOIN", 동일한 sessionUuid 사용)
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

    Set<UUID> watcherIds = getWatcherIds(contentId);
    log.info("[DEBUG-WATCH] 이벤트 발송 시도 - contentId: {}, type: JOIN, 현재 시청자 IDs: {}, 이벤트 내용: {}",
        contentId, watcherIds, changeEvent);
    messagingTemplate.convertAndSend("/sub/contents/" + contentId.toString() + "/watch", changeEvent);

    for (UUID watcherId : watcherIds) {
      sseService.send(watcherId, "watch", changeEvent);
    }

    return watcherCount;
  }

  //유저가 콘텐츠 시청을 종료(퇴장)할 때 Redis에서 세션을 제거하고 DB 갱신
  @Transactional
  public Long leaveSession(UUID userId, UUID contentId) {
    String userKey = USER_WATCHING_KEY_PREFIX + userId.toString();
    String contentKey = CONTENT_WATCHERS_KEY_PREFIX + contentId.toString();
    String sessionIdKey = USER_SESSION_ID_KEY_PREFIX + userId.toString();

    //삭제하기 직전의 시청자 목록 미리 확보(나간 당사자 본인도 퇴장 이벤트를 받아야 하므로 필수)
    Set<UUID> watcherIds = getWatcherIds(contentId);

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
          operations.opsForSet().remove(contentKey, userId.toString());

          List<Object> execResult = operations.exec();
          if (execResult == null || execResult.isEmpty()) {
            return null; // 충돌 발생, 재시도
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
      Long currentCount = redisTemplate.opsForSet().size(contentKey);
      return currentCount != null ? currentCount : 0L;
    }

    Long remainingCount = redisTemplate.opsForSet().size(contentKey);
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

    // 퇴장 이벤트 생성 (입장 시 사용했던 sessionUuid를 그대로 사용하여 프론트엔드가 목록에서 정상 제거하도록 함)
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

    // 웹소켓 브로드캐스팅
    messagingTemplate.convertAndSend("/sub/contents/" + contentId.toString() + "/watch", changeEvent);

    // 미리 확보해둔 watcherIds(나간 본인 포함)에게 SSE 발송
    for (UUID watcherId : watcherIds) {
      sseService.send(watcherId, "watch", changeEvent);
    }

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