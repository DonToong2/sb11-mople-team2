package com.codeit.mople.domain.user.repository.search;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
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

  // 검색어는 이메일만 사용
  @Field(
      type = FieldType.Text,
      analyzer = "user_ngram_analyzer",
      searchAnalyzer = "user_search_analyzer"
  )
  private String email;

  // 이름순
  @Field(type = FieldType.Keyword)
  private String name;

  // 생성순
  @Field(type = FieldType.Date, format = DateFormat.date_time)
  private Instant createdAt;

  // 잠김 정렬
  @Field(type = FieldType.Boolean)
  private Boolean locked;

  // 권한 정렬
  @Field(type = FieldType.Keyword)
  private String role;

  public UserDocument(
      UUID id,
      String email,
      String name,
      Instant createdAt,
      Boolean locked,
      String role
  ) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.createdAt = createdAt;
    this.locked = locked;
    this.role = role;
  }
}