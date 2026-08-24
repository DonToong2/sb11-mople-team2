package com.codeit.mople.domain.user.repository.search;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@NoArgsConstructor
@Document(indexName = "users")
@Setting(settingPath = "/elasticsearch/user-settings.json")
public class UserDocument {

  @Id
  private UUID id;

  @Field(
      type = FieldType.Text,
      analyzer = "user_ngram_analyzer",
      searchAnalyzer = "user_search_analyzer"
  )
  private String email;

  public UserDocument(UUID id, String email) {
    this.id = id;
    this.email = email;
  }

}
