package com.codeit.mople.domain.playlist.exception;

import com.codeit.mople.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {

  PLAYLIST_NOT_FOUND(HttpStatus.BAD_REQUEST, "PLAYLIST-001", "플레이리스트가 존재하지 않습니다."),
  PLAYLIST_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "PLAYLIST-002", "인증이 필요합니다."),
  PLAYLIST_DUPLICATE(HttpStatus.BAD_REQUEST, "PLAYLIST-003", "이미 구독한 플레이리스트입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
