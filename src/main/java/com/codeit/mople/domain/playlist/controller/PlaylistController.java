package com.codeit.mople.domain.playlist.controller;

import com.codeit.mople.domain.playlist.controller.api.PlaylistApi;
import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.service.PlaylistService;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController implements PlaylistApi {

  private final PlaylistService playlistService;
  private final UserRepository userRepository;

  @Override
  @PostMapping
  public ResponseEntity<PlaylistResponse> create(
      @RequestParam UUID ownerId, // TODO 김명근: 인증 구현 시 @AuthenticationPrincipal로 대체
      @Valid @RequestBody PlaylistCreateRequest request
  ) {

    // TODO 김명근: Custom UserDetails 등 인증 구현 시 해당 findById 삭제(UserRepository Import도 같이 삭제)
    User owner = userRepository.findById(ownerId).orElseThrow(() ->
            new CustomException(UserErrorCode.USER_NOT_FOUND));

    PlaylistResponse response = playlistService.create(owner, request);

    return ResponseEntity
        .created(URI.create("/api/playlists/" + response.id()))
        .body(response);
  }

  @GetMapping("/{playlistId}")
  public ResponseEntity<PlaylistResponse> find(
      @PathVariable UUID playlistId
  ) {
    PlaylistResponse response = playlistService.find(playlistId);

    return ResponseEntity.ok(response);
  }

}
