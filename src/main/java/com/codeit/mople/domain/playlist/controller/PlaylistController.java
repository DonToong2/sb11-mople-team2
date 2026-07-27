package com.codeit.mople.domain.playlist.controller;

import com.codeit.mople.domain.playlist.controller.api.PlaylistApi;
import com.codeit.mople.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import com.codeit.mople.domain.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @Override
  @PostMapping
  public ResponseEntity<PlaylistResponse> create(
      @RequestParam UUID ownerId,
      @Valid @RequestBody PlaylistCreateRequest request
  ) {

    PlaylistResponse response = playlistService.create(ownerId, request);

    return ResponseEntity
        .created(URI.create("/api/playlists/" + response.id()))
        .body(response);
  }

}
