package com.codeit.mople.domain.playlist.repository;

import com.codeit.mople.domain.playlist.entity.Playlist;
import com.codeit.mople.domain.playlist.repository.querydsl.PlaylistCustomRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends
    JpaRepository<Playlist, UUID>,
    PlaylistCustomRepository {

}
