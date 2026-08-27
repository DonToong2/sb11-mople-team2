package com.codeit.mople.domain.directmessage.repository;

import com.codeit.mople.domain.directmessage.document.DirectMessageDocument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DirectMessageSearchRepository extends
    ElasticsearchRepository<DirectMessageDocument, UUID> {

  @Query("""
      {
        "bool": {
          "must": [
            { "match_phrase": { "content": "?0" } }
          ],
          "filter": [
            { "terms": { "conversationId": ?1 } }
          ]
        }
      }
      """)
  List<DirectMessageDocument> findByContentMatchesAndConversationIdIn(
      String keyword,
      Collection<String> myConversationIds,
      Pageable pageable);

  void deleteByCreatedAtBefore(Instant createdAt);

}
