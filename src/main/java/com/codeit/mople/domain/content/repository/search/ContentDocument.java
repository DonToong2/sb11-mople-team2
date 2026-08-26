package com.codeit.mople.domain.content.repository.search;

import com.codeit.mople.domain.content.entity.ContentType;
import java.time.Instant;
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
  @Field(type = FieldType.Keyword)
  private UUID id;

  @Field(
      type = FieldType.Text,
      analyzer = "content_ngram_analyzer",
      searchAnalyzer = "content_search_analyzer")
  private String title;

  @Field(type = FieldType.Keyword)
  private ContentType type;

  // 평점순
  @Field(type = FieldType.Double)
  private double rating;

  // 인기순
  @Field(type = FieldType.Long)
  private long watcherCount;

  // 최신순
  @Field(type = FieldType.Date)
  private Instant createdAt;

  public ContentDocument(
      UUID id,
      String title,
      ContentType type,
      double rating,
      long watcherCount,
      Instant createdAt
  ) {
    this.id = id;
    this.title = title;
    this.type = type;
    this.rating = rating;
    this.watcherCount = watcherCount;
    this.createdAt = createdAt;
  }

}
