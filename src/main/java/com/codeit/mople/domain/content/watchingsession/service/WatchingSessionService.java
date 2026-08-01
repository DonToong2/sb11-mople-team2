package com.codeit.mople.domain.content.watchingsession.service;

import com.codeit.mople.domain.content.exception.ContentErrorCode;
import com.codeit.mople.domain.content.exception.ContentException;
import com.codeit.mople.domain.content.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mople.domain.content.watchingsession.dto.WatcherDto;
import com.codeit.mople.domain.content.watchingsession.dto.WatchingSessionContentDto;
import com.codeit.mople.domain.content.watchingsession.dto.WatchingSessionResponse;
import com.codeit.mople.domain.content.watchingsession.entity.WatchingSession;
import com.codeit.mople.domain.content.watchingsession.repository.WatchingSessionQueryRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionService {
  private final WatchingSessionQueryRepository watchingSessionQueryRepository;

  @Transactional(readOnly = true)
  public CursorResponseWatchingSessionDto getWatchingSessions(
      UUID contentId, String watcherNameLike, String cursor, UUID idAfter,
      int limit, String sortDirection, String sortBy) {
    log.debug("시청 세션 목록 조회 시작 - contentId: {}", contentId);

    if (limit <= 0 || limit > 100) {
      throw new ContentException(ContentErrorCode.INVALID_PAGE_REQUEST, Map.of("limit", limit));
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
            new WatcherDto(
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
}
