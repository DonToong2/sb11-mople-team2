package com.codeit.mople.domain.content.repository.search;

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
@Document(indexName = "contents")
@Setting(settingPath = "/elasticsearch/content-settings.json")
public class ContentDocument {

  @Id
  private UUID id;

  @Field(
      type = FieldType.Text,
      analyzer = "content_ngram_analyzer",
      searchAnalyzer = "content_search_analyzer")
  private String title;

  public ContentDocument(UUID id, String title) {
    this.id = id;
    this.title = title;
  }

}
