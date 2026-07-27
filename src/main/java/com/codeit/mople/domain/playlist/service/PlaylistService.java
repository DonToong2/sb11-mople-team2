package com.codeit.mople.domain.playlist.service;

import com.codeit.mople.domain.playlist.dto.response.PlaylistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistService {

  @Transactional
  public PlaylistResponse create() {
    return null;
  }

}
