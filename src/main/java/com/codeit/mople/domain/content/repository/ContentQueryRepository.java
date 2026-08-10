package com.codeit.mople.domain.content.repository;

import static com.codeit.mople.domain.content.entity.QContent.content;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentQueryRepository {
  private final JPAQueryFactory queryFactory;

  //커서 기반 데이터 조회 (limit + 1개)
  public List<Content> findContentByCursor(UUID cursorId, String cursorValue,
      int limit, ContentType type, String sortBy) {
    return queryFactory.selectFrom(content)
        .where(
            typeCondition(type), //카테고리 동적 필터
            cursorCondition(cursorId, cursorValue, sortBy) //정렬 기준별 커서 동적 조건
        )
        .orderBy(orderSpecifiers(sortBy)) //동적 OrderBy
        .limit(limit + 1)
        .fetch();
  }

  //전체 데이터 개수 조회
  public long countAllContents() {
    Long count = queryFactory.select(content.count())
        .from(content)
        .fetchOne();
    return count != null ? count : 0L;
  }

  //분류(type)별 데이터 개수 조회 메서드
  public long countContentsByType(ContentType type) {
    Long count = queryFactory.select(content.count())
        .from(content)
        .where(typeCondition(type))
        .fetchOne();
    return count != null ? count : 0L;
  }

  //카테고리 필터링 조건
  private BooleanExpression typeCondition(ContentType type) {
    return type != null ? content.type.eq(type) : null;
  }

  // 커서 필터링 조건
  private BooleanExpression cursorCondition(UUID cursorId, String cursorValue, String sortBy) {
    if (cursorId == null || cursorValue == null || cursorValue.isBlank()) {
      return null;
    }

    if ("watcherCount".equals(sortBy)) {
      long count = Long.parseLong(cursorValue);
      return content.watcherCount.lt(count)
          .or(content.watcherCount.eq(count).and(content.id.gt(cursorId)));
    } else if ("averageRating".equals(sortBy) || "rating".equals(sortBy) || "score".equals(sortBy) || "rate".equals(sortBy)) {
      double rating = Double.parseDouble(cursorValue);
      return content.averageRating.lt(rating)
          .or(content.averageRating.eq(rating).and(content.id.gt(cursorId)));
    } else { // 기본값: 최신순 (createdAt)
      Instant time = Instant.parse(cursorValue);
      return content.createdAt.lt(time)
          .or(content.createdAt.eq(time).and(content.id.gt(cursorId)));
    }
  }

  // 동적 정렬 조건 메서드
  private OrderSpecifier<?>[] orderSpecifiers(String sortBy) {
    if ("watcherCount".equals(sortBy)) {
      return new OrderSpecifier<?>[]{content.watcherCount.desc().nullsLast(), content.id.asc()};
    } else if ("averageRating".equals(sortBy) || "rating".equals(sortBy) || "score".equals(sortBy) || "rate".equals(sortBy)) {
      return new OrderSpecifier<?>[]{content.averageRating.desc().nullsLast(), content.id.asc()};
    }
    return new OrderSpecifier<?>[]{content.createdAt.desc().nullsLast(), content.id.asc()};
  }
}