package com.codeit.mople.domain.playlist.service;

import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistOwnerResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.entity.Playlist;
import com.codeit.mople.domain.playlist.entity.PlaylistSubscription;
import com.codeit.mople.domain.playlist.event.PlaylistSubscriptionCreateEvent;
import com.codeit.mople.domain.playlist.exception.PlaylistErrorCode;
import com.codeit.mople.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mople.domain.playlist.mapper.PlaylistOwnerMapper;
import com.codeit.mople.domain.playlist.repository.PlaylistRepository;
import com.codeit.mople.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final UserRepository userRepository;
  private final PlaylistOwnerMapper ownerMapper;
  private final PlaylistMapper mapper;
  private final ApplicationEventPublisher publisher;


  @Transactional
  public PlaylistResponse create(User owner, PlaylistCreateRequest request) {

    log.debug("플레이리스트 생성 시도: ownerId={}, title={}",
        owner.getId(), request.title());

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

  @Transactional
  public void subscribe(UUID playlistId, UUID subscriberId) {

    log.debug("플레이리스트 구독 시도: playlistId={}, subscriberId={}", playlistId, subscriberId);

    // 존재확인
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new CustomException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

    // 본인 구독 차단
    if (subscriberId.equals(playlist.getOwner().getId())) {
      throw new CustomException(PlaylistErrorCode.PLAYLIST_DUPLICATE);
    }

    // 존재확인
    User subscriber = userRepository.findById(subscriberId)
        .orElseThrow(() -> new CustomException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

    // 중복 구독 차단
    if (playlistSubscriptionRepository.existsByPlaylistIdAndSubscriberId(playlistId, subscriberId)) {
      throw new CustomException(PlaylistErrorCode.PLAYLIST_DUPLICATE);
    }

    PlaylistSubscription saved = playlistSubscriptionRepository.save(
        PlaylistSubscription.create(playlist, subscriber));

    log.info("플레이리스트 구독 성공: playlistSubscriptionId={}, playlistId={}, subscriberId={}",
        saved.getId(), playlistId, subscriberId);

    publisher.publishEvent(new PlaylistSubscriptionCreateEvent(playlistId, subscriberId));
  }

}
