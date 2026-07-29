package com.codeit.mople.domain.playlist.exception;

import com.codeit.mople.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {
  PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST-001", "플레이리스트를 찾을 수 없습니다."),
  PLAYLIST_FORBIDDEN(HttpStatus.FORBIDDEN, "PLAYLIST-002", "플레이리스트에 대한 접근 권한이 없습니다."),
  PLAYLIST_UPDATE_BLANK_TITLE(HttpStatus.BAD_REQUEST, "PLAYLIST-003", "제목을 입력해주세요."),
  PLAYLIST_UPDATE_BLANK_DESCRIPTION(HttpStatus.BAD_REQUEST, "PLAYLIST-004", "설명을 입력해주세요.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
