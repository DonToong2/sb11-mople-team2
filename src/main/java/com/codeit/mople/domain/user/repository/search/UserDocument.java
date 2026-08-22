package com.codeit.mople.domain.user.repository.search;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Getter
@NoArgsConstructor
@Document(indexName = "users")
public class UserDocument {

  @Id
  private UUID id;

  private String email;

  public UserDocument(UUID id, String email) {
    this.id = id;
    this.email = email;
  }

}
