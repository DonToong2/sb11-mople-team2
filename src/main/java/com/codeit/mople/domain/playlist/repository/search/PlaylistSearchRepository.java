package com.codeit.mople.domain.playlist.repository.search;

import java.util.List;
import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PlaylistSearchRepository extends ElasticsearchRepository<PlaylistDocument, UUID> {

  List<PlaylistDocument> findByTitleContainingIgnoreCase(String title);
}
