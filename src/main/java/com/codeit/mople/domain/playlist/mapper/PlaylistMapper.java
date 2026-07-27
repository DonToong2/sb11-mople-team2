package com.codeit.mople.domain.playlist.mapper;

import com.codeit.mople.domain.playlist.dto.response.PlaylistContentResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistOwnerResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.entity.Playlist;
import java.util.List;

public class PlaylistMapper {

  public PlaylistResponse toResponse(
      Playlist playlist, PlaylistOwnerResponse owner,
      boolean subscribedByMe,
      List<PlaylistContentResponse> contents
  ) {
    return new PlaylistResponse(
        playlist.getId(),
        owner,
        playlist.getTitle(),
        playlist.getDescription(),
        playlist.getUpdatedAt(),
        playlist.getSubscriberCount(),
        subscribedByMe,
        contents
    );
  }
}
