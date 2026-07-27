package com.codeit.mople.domain.playlist.service;

import static com.codeit.mople.domain.user.entity.QUser.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
  private PlaylistOwnerMapper ownerMapper;

  @Mock
  private PlaylistMapper mapper;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private PlaylistService playlistService;

  private UUID ownerId;
  private String title;
  private String description;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    title = "새 플레이리스트 (1)";
    description = "새로운 플레이리스트입니다.";
  }

  @Test
  @DisplayName("플레이리스트 생성 성공")
  void create_success() {
    // given

    // setUp()에서 ownerId, title, description 초기화

    PlaylistCreateRequest request = new PlaylistCreateRequest(title, description);

    Playlist playlist = Playlist.create(ownerId, title, description);

    User owner = mock(User.class);

    PlaylistOwnerResponse ownerResponse = new PlaylistOwnerResponse(
        ownerId,
        "사용자",
        "profileImageUrl"
    );

    PlaylistResponse response = mock(PlaylistResponse.class);

    // playlist DB 저장 → user DB 조회 → PlaylistOwnerMapper 생성 → PlaylistMapper 생성 순
    // 성공 테스트이기 때문에 orElseThrow() 미호출
    given(userRepository.findById(ownerId))
        .willReturn(Optional.of(owner));

    given(playlistRepository.save(any(Playlist.class)))
        .willReturn(playlist);

    given(ownerMapper.toResponse(owner))
        .willReturn(ownerResponse);

    given(mapper.toResponse(
        any(Playlist.class),
        eq(ownerResponse),
        eq(true),
        eq(List.of())
    ))
        .willReturn(response);

    // when
    PlaylistResponse result = playlistService.create(ownerId, request);

    // then
    // 결과 중심
    assertThat(result).isEqualTo(response);

    // 행위 중심(given(...) 메서드가 호출됐는지 검증)
    verify(playlistRepository).save(any(Playlist.class));
    verify(userRepository).findById(ownerId);
    verify(ownerMapper).toResponse(owner);
    verify(mapper).toResponse(
        any(Playlist.class),
        eq(ownerResponse),
        eq(true),
        eq(List.of())
    );
  }

  @Test
  @DisplayName("플레이리스트 생성 실패 - 사용자가 존재하지 않음")
  void create_fail_notFoundUser() {
    // given

    // setUp()에서 ownerId, title, description 초기화

    PlaylistCreateRequest request = new PlaylistCreateRequest(title, description);

    given(userRepository.findById(ownerId))
        // TODO 김명근: UserNotFound 예외 추가 시 .willReturn() 메서드로 교체 후 해당 예외 클래스 추가하여 리팩토링
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() ->
        // when
        playlistService.create(ownerId, request))
        // then
        // TODO 김명근: UserNotFound 예외 추가 시 isInstanceOf()에 해당 예외 클래스 추가하여 리팩토링
        .isInstanceOf(NoSuchElementException.class);
  }

}
