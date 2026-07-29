package com.codeit.mople.domain.playlist.exception;

import java.util.Map;
import java.util.UUID;

public class PlaylistUpdateBlankTitleException extends PlaylistException {

  public PlaylistUpdateBlankTitleException(UUID playlistId) {
    super(PlaylistErrorCode.PLAYLIST_UPDATE_BLANK_TITLE, Map.of("playlistId", playlistId));
  }
}
