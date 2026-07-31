package com.codeit.mople.domain.user.repository;

import com.codeit.mople.domain.user.dto.request.UserSearchRequest;
import com.codeit.mople.domain.user.entity.QUser;
import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.global.dto.SortDirection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom{

  private final JPAQueryFactory queryFactory;

  public UserRepositoryImpl(JPAQueryFactory queryFactory) {
    this.queryFactory = queryFactory;
  }

  private static final QUser user = QUser.user;

  @Override
  public List<User> searchUsers(UserSearchRequest request) {
    boolean isAsc = request.sortDirectionOrDefault() == SortDirection.ASCENDING;

    BooleanBuilder filters = new BooleanBuilder();
    if(request.emailLike() != null && !request.emailLike().isBlank()) {
      filters.and(user.email.containsIgnoreCase(request.emailLike()));
    }
    if(request.roleEqual() != null) {
      filters.and(user.role.eq(request.roleEqual()));
    }
    if(request.isLocked() != null) {
      filters.and(user.locked.eq(request.isLocked()));
    }
    BooleanExpression cursorCondition = cursorCondition(request, isAsc);
    if(cursorCondition != null) {
      filters.and(cursorCondition);
    }

    return queryFactory
        .selectFrom(user)
        .where(filters)
        .orderBy(orderSpecifiers(request, isAsc))
        .limit(request.limitOrDefault() + 1L)
        .fetch();
  }

  private BooleanExpression cursorCondition(UserSearchRequest request, boolean isAsc) {
    if(request.cursor() == null || request.idAfter() == null) {
      return null;
    }

    UUID idAfter = request.idAfter();
    String cursor = request.cursor();

    return switch (request.sortByOrDefault()) {
      case name -> isAsc
          ? user.name.gt(cursor).or(user.name.eq(cursor).and(user.id.gt(idAfter)))
          : user.name.lt(cursor).or(user.name.eq(cursor).and(user.id.lt(idAfter)));
      case email -> isAsc
          ? user.email.gt(cursor).or(user.email.eq(cursor).and(user.id.gt(idAfter)))
          : user.email.lt(cursor).or(user.email.eq(cursor).and(user.id.lt(idAfter)));
      case createdAt -> {
        Instant cursorTime = Instant.parse(cursor);
        yield isAsc
            ? user.createdAt.gt(cursorTime).or(user.createdAt.eq(cursorTime).and(user.id.gt(idAfter)))
            : user.createdAt.lt(cursorTime).or(user.createdAt.eq(cursorTime).and(user.id.lt(idAfter)));
      }
      case isLocked -> {
        boolean cursorLocked = Boolean.parseBoolean(cursor);
        yield user.locked.eq(cursorLocked).and(isAsc ? user.id.gt(idAfter) : user.id.lt(idAfter));
      }
      case role -> {
        Role cursorRole = Role.valueOf(cursor);
        yield user.role.eq(cursorRole).and(isAsc ? user.id.gt(idAfter) : user.id.lt(idAfter));
      }
    };
  }

  private OrderSpecifier<?>[] orderSpecifiers(UserSearchRequest request, boolean isAsc) {
    OrderSpecifier<?> primary = switch (request.sortByOrDefault()) {
      case name -> isAsc ? user.name.asc() : user.name.desc();
      case email -> isAsc ? user.email.asc() : user.email.desc();
      case createdAt -> isAsc ? user.createdAt.asc() : user.createdAt.desc();
      case isLocked -> isAsc ? user.locked.asc() : user.locked.desc();
      case role -> isAsc ? user.role.asc() : user.role.desc();
    };
    OrderSpecifier<?> secondary = isAsc ? user.id.asc() : user.id.desc();
    return new OrderSpecifier[]{primary, secondary};
  }
}
