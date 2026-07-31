package com.codeit.mople.domain.playlist.repository;

import com.codeit.mople.domain.playlist.dto.request.PlaylistQueryCondition;
import com.codeit.mople.domain.playlist.entity.Playlist;
import java.util.List;

public interface PlaylistRepositoryCustom {

  List<Playlist> findAll(PlaylistQueryCondition condition);

}
