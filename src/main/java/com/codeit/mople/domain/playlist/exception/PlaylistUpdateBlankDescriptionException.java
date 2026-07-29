package com.codeit.mople.domain.playlist.exception;

import java.util.Map;
import java.util.UUID;

public class PlaylistUpdateBlankDescriptionException extends PlaylistException {

  public PlaylistUpdateBlankDescriptionException(UUID playlistId) {
    super(PlaylistErrorCode.PLAYLIST_UPDATE_BLANK_DESCRIPTION, Map.of("playlistId", playlistId));
  }
}
