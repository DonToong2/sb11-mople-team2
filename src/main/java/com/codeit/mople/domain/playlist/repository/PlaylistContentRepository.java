package com.codeit.mople.domain.playlist.repository;

import com.codeit.mople.domain.playlist.entity.PlaylistContent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

  @Query("""
      select pc
      from PlaylistContent pc
      join fetch pc.content
      where pc.playlist.id = :playlistId
      order by pc.createdAt asc
      """)
  List<PlaylistContent> findAllByPlaylistIdOrderByCreatedAtAsc(
      @Param("playlistId") UUID playlistId);
}
