package com.codeit.mople.domain.playlist.repository.search;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@NoArgsConstructor
@Document(indexName = "playlists")
@Setting(settingPath = "/elasticsearch/playlist-settings.json")
public class PlaylistDocument {

  @Id
  @Field(type = FieldType.Keyword)
  private UUID id;

  @MultiField(
      mainField = @Field(
          type = FieldType.Text,
          analyzer = "playlist_ngram_analyzer",
          searchAnalyzer = "playlist_search_analyzer"
      ),
      otherFields = {
          @InnerField(
              suffix = "keyword",
              type = FieldType.Keyword
          )
      }
  )
  private String title;

  // 구독순
  @Field(type = FieldType.Long)
  private long subscribeCount;

  // 최신순
  @Field(type = FieldType.Date)
  private Instant updatedAt;

  public PlaylistDocument(UUID id, String title, long subscribeCount, Instant updatedAt) {
    this.id = id;
    this.title = title;
    this.subscribeCount = subscribeCount;
    this.updatedAt = updatedAt;
  }

}
