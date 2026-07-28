package com.codeit.mople.domain.playlist.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PlaylistTest {

  @Test
  @DisplayName("플레이리스트 생성 성공")
  void create_success() {
    // given
    User owner = User.createUser("test@test.com", "12345678", "test");
    String title = "새 플레이리스트 (1)";
    String description = "새로운 플레이리스트입니다.";

    // when
    Playlist playlist = Playlist.create(owner, title, description);

    // then
    assertThat(playlist.getOwner()).isEqualTo(owner);
    assertThat(playlist.getTitle()).isEqualTo(title);
    assertThat(playlist.getDescription()).isEqualTo(description);
    assertThat(playlist.getSubscriberCount()).isEqualTo(0L);
  }
}
