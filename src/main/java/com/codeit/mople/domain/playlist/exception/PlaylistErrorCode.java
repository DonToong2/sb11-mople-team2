package com.codeit.mople.domain.playlist.exception;

import com.codeit.mople.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {
  PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST-001", "플레이리스트를 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
