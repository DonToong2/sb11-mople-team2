package com.codeit.mople.domain.playlist.repository.search;

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
@Document(indexName = "playlists")
@Setting(settingPath = "/elasticsearch/playlist-settings.json")
public class PlaylistDocument {

  @Id
  private UUID id;

  @Field(
      type = FieldType.Text,
      analyzer = "playlist_ngram_analyzer",
      searchAnalyzer = "playlist_search_analyzer"
  )
  private String title;

  public PlaylistDocument(UUID id, String title) {
    this.id = id;
    this.title = title;
  }

}
