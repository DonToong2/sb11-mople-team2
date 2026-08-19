package com.codeit.mople.domain.follow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.follow.entity.Follow;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.global.config.JpaAuditingConfig;
import com.codeit.mople.global.config.QueryDslConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
@DisplayName("FollowRepository 테스트")
class FollowRepositoryTest {

  @Autowired
  FollowRepository followRepository;

  @Autowired
  TestEntityManager entityManager;

  User followee;
  User follower;
  User other;

  @BeforeEach
  void setUp() {
    followee = entityManager.persist(User.createUser("followw@mople.com", "password", "팔로우 대상"));
    follower = entityManager.persist(User.createUser("follower@mople.com", "password", "팔로워"));
    other = entityManager.persist(User.createUser("other@mople.com", "password", "제3자"));
  }

  @Test
  @DisplayName("followeeId를 집어 넣으면 followeeId를 가지고 있는 followerId를 잘 뽑아내는지")
  void returnFollowersIdsOnly() {
    entityManager.persist(Follow.create(followee, follower));
    entityManager.persist(Follow.create(followee, other));
    entityManager.flush();
    entityManager.clear();
    // when
    List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(followee.getId());

    // then
    assertThat(followerIds).containsExactlyInAnyOrder(follower.getId(), other.getId());
  }


}
