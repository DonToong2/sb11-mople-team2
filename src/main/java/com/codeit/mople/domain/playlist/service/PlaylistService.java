package com.codeit.mople.domain.playlist.service;

import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistContentResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistOwnerResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.entity.Playlist;
import com.codeit.mople.domain.playlist.exception.PlaylistErrorCode;
import com.codeit.mople.domain.playlist.mapper.PlaylistContentMapper;
import com.codeit.mople.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mople.domain.playlist.mapper.PlaylistOwnerMapper;
import com.codeit.mople.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.mople.domain.playlist.repository.PlaylistRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final PlaylistOwnerMapper ownerMapper;
  private final PlaylistContentMapper playlistContentMapper;
  private final PlaylistMapper mapper;

  @Transactional
  public PlaylistResponse create(UUID ownerId, PlaylistCreateRequest request) {

    log.debug("플레이리스트 생성 시도: ownerId={}, title={}",
        ownerId, request.title());

    User owner = userRepository.findById(ownerId).orElseThrow(() ->
        new CustomException(UserErrorCode.USER_NOT_FOUND));

    Playlist playlist = Playlist.create(owner, request.title(), request.description());

    Playlist savedPlaylist = playlistRepository.save(playlist);

    PlaylistOwnerResponse ownerResponse = ownerMapper.toResponse(owner);

    PlaylistResponse response = mapper.toResponse(
        savedPlaylist,
        ownerResponse,
        false,
        List.of());

    log.info("플레이리스트 생성 완료: playlistId={}, ownerId={}",
        savedPlaylist.getId(), owner.getId());

    return response;
  }

  // 플레이리스트 세부 조회(단건 조회)
  @Transactional(readOnly = true)
  public PlaylistResponse find(UUID playlistId) {

    log.debug("플레이리스트 조회 시도: playlistId={}",
        playlistId);

    Playlist playlist = playlistRepository.findById(playlistId).orElseThrow(() ->
        new CustomException(PlaylistErrorCode.PLAYLIST_NOT_FOUND)
    );

    PlaylistOwnerResponse ownerResponse = ownerMapper.toResponse(playlist.getOwner());

    // 콘텐츠를 플레이리스트에 추가한 순서대로 표시(콘텐츠를 B, E, A, C 순으로 추가했을 경우 추가한 순서 그대로)
    List<PlaylistContentResponse> contents =
        playlistContentRepository.findAllByPlaylistIdOrderByCreatedAtAsc(playlistId).stream()
            .map(playlistContentMapper::toResponse)
            .toList();

    PlaylistResponse response = mapper.toResponse(
        playlist,
        ownerResponse,
        false,
        contents);

    log.info("플레이리스트 조회 완료: playlistId={}, contentCount={}",
        playlistId, contents.size());

    return response;
  }

  @Transactional
  public PlaylistResponse update(
      UUID playlistId,
      PlaylistUpdateRequest request,
      UUID userId
  ) {

    log.debug("플레이리스트 수정 시도: playlistId={}, userId={}",
        playlistId, userId);

    // Playlist 조회
    Playlist playlist = playlistRepository.findById(playlistId).orElseThrow(() ->
        new CustomException(PlaylistErrorCode.PLAYLIST_NOT_FOUND)
    );

    // 플레이리스트 소유자가 맞는지 검증
    validateOwner(playlist, userId);

    playlist.update(request.title(), request.description());

    PlaylistOwnerResponse ownerResponse = ownerMapper.toResponse(playlist.getOwner());

    List<PlaylistContentResponse> contents =
        playlistContentRepository.findAllByPlaylistIdOrderByCreatedAtAsc(playlistId).stream()
            .map(playlistContentMapper::toResponse)
            .toList();

    PlaylistResponse response = mapper.toResponse(
        playlist,
        ownerResponse,
        false,
        contents
    );

    log.info("플레이리스트 수정 완료: playlistId={}, userId={}",
        playlistId, userId);

    return response;
  }

  @Transactional
  public void delete(UUID playlistId, UUID userId) {

    log.debug("플레이리스트 삭제 시도: playlistId={}, userId={}",
        playlistId, userId);

    Playlist playlist = playlistRepository.findById(playlistId).orElseThrow(() ->
        new CustomException(PlaylistErrorCode.PLAYLIST_NOT_FOUND)
    );

    validateOwner(playlist, userId);

    // deleteById도 가능하지만 where id=로 조회 후 delete하기 때문에(불필요한 조회가 발생함) 조회 실행을 뺌
    playlistRepository.delete(playlist);

    log.info("플레이리스트 삭제 완료: playlistId={}, userId={}",
        playlistId, userId);

  }

  private void validateOwner(Playlist playlist, UUID userId) {
    if (!playlist.getOwner().getId().equals(userId)) {
      throw new CustomException(PlaylistErrorCode.PLAYLIST_FORBIDDEN);
    }
  }
}
