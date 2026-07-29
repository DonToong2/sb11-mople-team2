package com.codeit.mople.domain.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistContentResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistOwnerResponse;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.entity.Playlist;
import com.codeit.mople.domain.playlist.entity.PlaylistContent;
import com.codeit.mople.domain.playlist.exception.PlaylistErrorCode;
import com.codeit.mople.domain.playlist.mapper.PlaylistContentMapper;
import com.codeit.mople.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mople.domain.playlist.mapper.PlaylistOwnerMapper;
import com.codeit.mople.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.mople.domain.playlist.repository.PlaylistRepository;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.global.error.CustomException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlaylistServiceTest {

  @Mock
  private PlaylistRepository playlistRepository;

  @Mock
  private PlaylistContentRepository playlistContentRepository;

  @Mock
  private PlaylistMapper mapper;

  @Mock
  private PlaylistOwnerMapper ownerMapper;

  @Mock
  private PlaylistContentMapper playlistContentMapper;

  @InjectMocks
  private PlaylistService playlistService;

  private User owner;
  private UUID ownerId;
  private String title;
  private String description;

  private Playlist mockPlaylist;
  private Playlist playlist;
  private UUID playlistId;

  private PlaylistUpdateRequest updateRequest;

  @BeforeEach
  void setUp() {
    owner = mock(User.class);
    ownerId = UUID.randomUUID();
    title = "새 플레이리스트 (1)";
    description = "새로운 플레이리스트입니다.";

    mockPlaylist = mock(Playlist.class);
    playlistId = UUID.randomUUID();
    playlist = Playlist.create(owner, title, description);

    updateRequest = new PlaylistUpdateRequest("수정한 제목", "수정한 설명");
  }

  @Nested
  @DisplayName("플레이리스트 생성")
  class Create {

    @Test
    @DisplayName("플레이리스트 생성 성공")
    void create_success() {
      // given

      // setUp()에서 owner, ownerId, title, description 초기화

      PlaylistCreateRequest createRequest = new PlaylistCreateRequest(title, description);

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
      PlaylistResponse result = playlistService.create(owner, createRequest);

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
  }

  @Nested
  @DisplayName("플레이리스트 단건 조회")
  class Find {

    @Test
    @DisplayName("플레이리스트 단건 조회 성공")
    void find_success() {
      // given

      // BeforeEach에서 owner, playlist, playlistId를 초기화

      PlaylistOwnerResponse ownerResponse = mock(PlaylistOwnerResponse.class);

      PlaylistContent playlistContent = mock(PlaylistContent.class);
      PlaylistContentResponse playlistContentResponse = mock(PlaylistContentResponse.class);

      PlaylistResponse response = mock(PlaylistResponse.class);

      given(playlistRepository.findById(playlistId))
          .willReturn(Optional.of(mockPlaylist));

      given(mockPlaylist.getOwner())
          .willReturn(owner);
      given(ownerMapper.toResponse(owner))
          .willReturn(ownerResponse);

      given(playlistContentRepository.findAllByPlaylistIdOrderByCreatedAtAsc(playlistId))
          .willReturn(List.of(playlistContent));
      given(playlistContentMapper.toResponse(playlistContent))
          .willReturn(playlistContentResponse);

      given(mapper.toResponse(
          eq(mockPlaylist),
          eq(ownerResponse),
          eq(false),
          eq(List.of(playlistContentResponse))
      ))
          .willReturn(response);

      // when
      PlaylistResponse result = playlistService.find(playlistId);

      // then
      assertThat(result).isEqualTo(response);

      verify(playlistRepository).findById(playlistId);
      verify(ownerMapper).toResponse(owner);
      verify(playlistContentRepository).findAllByPlaylistIdOrderByCreatedAtAsc(playlistId);
      verify(playlistContentMapper).toResponse(playlistContent);
      verify(mapper).toResponse(
          eq(mockPlaylist),
          eq(ownerResponse),
          eq(false),
          eq(List.of(playlistContentResponse))
      );
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 실패 - 플레이리스트가 존재하지 않음")
    void find_fail_notFoundPlaylist() {
      // given
      UUID notExistPlaylistId = UUID.randomUUID();

      given(playlistRepository.findById(notExistPlaylistId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistService.find(notExistPlaylistId))
          .isInstanceOf(CustomException.class);

      verify(playlistRepository).findById(notExistPlaylistId);
      verifyNoInteractions(
          ownerMapper,
          playlistContentRepository,
          playlistContentMapper,
          mapper
      );
    }
  }

  @Nested
  @DisplayName("플레이리스트 수정")
  class Update {

    @Test
    @DisplayName("플레이리스트 수정 성공")
    void update_success() {
      // given

      // BeforeEach에서 updateRequest 초기화

      PlaylistOwnerResponse ownerResponse =
          mock(PlaylistOwnerResponse.class);

      PlaylistResponse response =
          mock(PlaylistResponse.class);

      given(playlistRepository.findById(playlistId))
          .willReturn(Optional.of(playlist));

      given(owner.getId())
          .willReturn(ownerId);

      given(ownerMapper.toResponse(owner))
          .willReturn(ownerResponse);

      given(playlistContentRepository.findAllByPlaylistIdOrderByCreatedAtAsc(playlistId))
          .willReturn(List.of());

      given(mapper.toResponse(
          eq(playlist),
          eq(ownerResponse),
          eq(false),
          eq(List.of())
      ))
          .willReturn(response);

      // when
      PlaylistResponse result = playlistService.update(playlistId, updateRequest, ownerId);

      // then
      assertThat(result).isEqualTo(response);
      assertThat(playlist.getTitle()).isEqualTo("수정한 제목");
      assertThat(playlist.getDescription()).isEqualTo("수정한 설명");
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 플레이리스트가 존재하지 않음")
    void update_fail_notFoundPlaylist() {
      // given
      given(playlistRepository.findById(playlistId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistService.update(playlistId, updateRequest, ownerId))
          .isInstanceOf(CustomException.class);

      verify(playlistRepository).findById(playlistId);

      verifyNoInteractions(
          ownerMapper,
          playlistContentRepository,
          playlistContentMapper,
          mapper
      );
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 플레이리스트 소유자가 아님")
    void update_fail_forbidden() {
      // given
      UUID noOwnerId = UUID.randomUUID();

      given(playlistRepository.findById(playlistId))
          .willReturn(Optional.of(playlist));

      given(owner.getId())
          .willReturn(ownerId);

      // when & then
      assertThatThrownBy(() -> playlistService.update(playlistId, updateRequest, noOwnerId))
          .isInstanceOf(CustomException.class);

      verify(playlistRepository).findById(playlistId);

      verifyNoInteractions(
          ownerMapper,
          playlistContentRepository,
          playlistContentMapper,
          mapper
      );
    }
  }

  @Nested
  @DisplayName("플레이리스트 삭제")
  class Delete {

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void delete_success() {
      // given

      // BeforeEach에서 playlist, playlistId 초기화
      given(playlistRepository.findById(playlistId))
          .willReturn(Optional.of(playlist));

      given(owner.getId())
          .willReturn(ownerId);

      // when
      playlistService.delete(playlistId, ownerId);

      // then
      // void 타입이기 때문에 assert 메서드 불필요
      verify(playlistRepository).findById(playlistId);
      verify(playlistRepository).delete(playlist);
    }

    @Test
    @DisplayName("플레이리스트 삭제 실패 - 플레이리스트가 존재하지 않음")
    void delete_fail_notFoundPlaylist() {
      // given
      given(playlistRepository.findById(playlistId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          playlistService.delete(playlistId, ownerId)
      )
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(PlaylistErrorCode.PLAYLIST_NOT_FOUND);

      verify(playlistRepository).findById(playlistId);

      // playlistRepository.delete() 메서드는 호출되지 않음
      verify(playlistRepository, never())
          .delete(any(Playlist.class));
    }

    @Test
    @DisplayName("플레이리스트 삭제 실패 - 플레이리스트 소유자가 아님")
    void delete_fail_forbidden() {
      // given
      UUID noOwnerId = UUID.randomUUID();

      given(playlistRepository.findById(playlistId))
          .willReturn(Optional.of(playlist));

      given(owner.getId())
          .willReturn(ownerId);

      // when & then
      assertThatThrownBy(() ->
          playlistService.delete(playlistId, noOwnerId)
      )
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(PlaylistErrorCode.PLAYLIST_FORBIDDEN);

      verify(playlistRepository).findById(playlistId);

      verify(playlistRepository, never())
          .delete(any(Playlist.class));
    }
  }

}
