package com.codeit.mople.domain.user.repository.search;

import java.util.List;
import java.util.UUID;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, UUID> {

  @Query("""
    {
      "wildcard": {
        "email": {
          "value": "*?0*"
        }
      }
    }
    """)
  List<UserDocument> findByEmailContainingIgnoreCase(String email);
}
