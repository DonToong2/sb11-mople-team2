package com.codeit.mople.domain.playlist.service;

import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistOwnerResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.entity.Playlist;
import com.codeit.mople.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mople.domain.playlist.mapper.PlaylistOwnerMapper;
import com.codeit.mople.domain.playlist.repository.PlaylistRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistOwnerMapper playlistOwnerMapper;
  private final PlaylistMapper playlistMapper;

  private final UserRepository userRepository;

  @Transactional
  public PlaylistResponse create(UUID ownerId, PlaylistCreateRequest request) {

    Playlist playlist = Playlist.create(ownerId, request.title(), request.description());

    Playlist savedPlaylist = playlistRepository.save(playlist);

    // TODO(김명근) : UserNotFound 예외 추가 시 .orElseThrow()에 해당 예외 클래스 추가하여 리팩토링
    User owner = userRepository.findById(ownerId).orElseThrow();
    PlaylistOwnerResponse ownerResponse = playlistOwnerMapper.toResponse(owner);

    return playlistMapper.toResponse(
        savedPlaylist,
        ownerResponse,
        true,
        List.of());
  }

}
