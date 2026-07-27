package com.codeit.mople.domain.playlist.entity;

import com.codeit.mople.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseTimeEntity {

  // userId
  @Column(nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private long subscriberCount = 0L;

  private Playlist(UUID ownerId, String title, String description) {
    this.ownerId = ownerId;
    this.title = title;
    this.description = description;
  }

  public static Playlist create(UUID ownerId, String title, String description) {
    return new Playlist(ownerId, title, description);
  }

}
