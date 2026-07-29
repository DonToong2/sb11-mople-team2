package com.codeit.mople.domain.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class PlaylistServiceTest {

  @Mock
  private PlaylistRepository playlistRepository;

  @Mock
  private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PlaylistOwnerMapper ownerMapper;

  @Mock
  private PlaylistMapper mapper;

  @Mock
  private ApplicationEventPublisher publisher;

  @InjectMocks
  private PlaylistService playlistService;


  private User owner;
  private UUID ownerId;
  private String title;
  private String description;

  @BeforeEach
  void setUp() {
    owner = mock(User.class);
    ownerId = UUID.randomUUID();
    title = "새 플레이리스트 (1)";
    description = "새로운 플레이리스트입니다.";
  }

  @Test
  @DisplayName("플레이리스트 생성 성공")
  void create_success() {
    // given

    // setUp()에서 owner, ownerId, title, description 초기화

    PlaylistCreateRequest request = new PlaylistCreateRequest(title, description);

    Playlist playlist = Playlist.create(owner, title, description);

    PlaylistOwnerResponse ownerResponse = new PlaylistOwnerResponse(
        ownerId,
        "사용자",
        "profileImageUrl"
    );

    PlaylistResponse response = mock(PlaylistResponse.class);

    // playlist DB 저장 → PlaylistOwnerMapper 생성 → PlaylistMapper 생성 순

    given(playlistRepository.save(any(Playlist.class)))
        .willReturn(playlist);

    given(ownerMapper.toResponse(owner))
        .willReturn(ownerResponse);

    given(mapper.toResponse(
        any(Playlist.class),
        eq(ownerResponse),
        eq(false),
        eq(List.of())
    ))
        .willReturn(response);

    // when
    PlaylistResponse result = playlistService.create(owner, request);

    // then
    // 결과 중심(상태 검증)
    assertThat(result).isEqualTo(response);

    // 행위 중심(given(...) 메서드가 호출됐는지 검증)
    verify(playlistRepository).save(any(Playlist.class));
    verify(ownerMapper).toResponse(owner);
    verify(mapper).toResponse(
        any(Playlist.class),
        eq(ownerResponse),
        eq(false),
        eq(List.of())
    );
  }

  @Test
  @DisplayName("플레이리스트 구독 성공")
  void subscribe_success() {
    // given
    UUID playlistId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();

    Playlist playlist = Playlist.create(owner, title, description);
    User subscriber = mock(User.class);

    given(playlistSubscriptionRepository.existsByPlaylistIdAndSubscriberId(playlistId, subscriberId)).willReturn(false);
    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
    given(playlistSubscriptionRepository.save(any(PlaylistSubscription.class))).willReturn(PlaylistSubscription.create(playlist, subscriber));

    // when
    playlistService.subscribe(playlistId, subscriberId);

    // then
    verify(playlistSubscriptionRepository).save(any(PlaylistSubscription.class));
    verify(publisher).publishEvent(any(PlaylistSubscriptionCreateEvent.class));
  }

  @Test
  @DisplayName("이미 구독한 플레이리스트면 예외")
  void subscribe_duplicate() {
    UUID playlistId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();
    Playlist playlist = Playlist.create(owner, title, description);
    User subscriber = mock(User.class);

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
    given(playlistSubscriptionRepository.existsByPlaylistIdAndSubscriberId(playlistId, subscriberId)).willReturn(true);

    assertThatThrownBy(() -> playlistService.subscribe(playlistId, subscriberId))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", PlaylistErrorCode.PLAYLIST_DUPLICATE);

    verify(playlistSubscriptionRepository, never()).save(any());
    verify(publisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("플래이리스트가 없으면 예외")
  void subscribe_playlistNotfound() {
    UUID playlistId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.subscribe(playlistId, subscriberId))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", PlaylistErrorCode.PLAYLIST_NOT_FOUND);

    verify(playlistSubscriptionRepository, never()).save(any());
    verify(publisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("구독자가 없으면 예외")
  void subscribe_userNotFound() {
    UUID playlistId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();
    Playlist playlist = Playlist.create(owner, title, description);

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(userRepository.findById(subscriberId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.subscribe(playlistId, subscriberId))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", PlaylistErrorCode.PLAYLIST_NOT_FOUND);

    verify(playlistSubscriptionRepository, never()).save(any());
    verify(publisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("본인 플레이리스트 구독 차단")
  void subscribe_selfSubScribe() {
    UUID playlistId = UUID.randomUUID();
    Playlist playlist = Playlist.create(owner, title, description);

    given(owner.getId()).willReturn(ownerId);
    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

    assertThatThrownBy(() -> playlistService.subscribe(playlistId, ownerId))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", PlaylistErrorCode.PLAYLIST_DUPLICATE);

    verify(userRepository, never()).findById(any());
    verify(playlistSubscriptionRepository, never()).save(any());
    verify(publisher, never()).publishEvent(any());
  }



}
