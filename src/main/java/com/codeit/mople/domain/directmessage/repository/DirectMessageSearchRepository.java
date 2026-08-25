package com.codeit.mople.domain.directmessage.repository;

import com.codeit.mople.domain.directmessage.document.DirectMessageDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DirectMessageSearchRepository extends ElasticsearchRepository<DirectMessageDocument, UUID> {

  @Query("{\"match_phrase\": {\"content\": \"?0\"}}")
  List<DirectMessageDocument> findByContentMatches(String keyword);

}
