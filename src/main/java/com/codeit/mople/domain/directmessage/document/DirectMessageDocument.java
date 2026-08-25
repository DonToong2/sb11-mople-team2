package com.codeit.mople.domain.directmessage.document;

import com.codeit.mople.domain.directmessage.entity.DirectMessage;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(indexName = "direct_messages")
@Setting(settingPath = "elasticsearch/dm-settings.json")
public class DirectMessageDocument {

  @Id
  @Field(type = FieldType.Keyword)
  private UUID id;

  @Field(type = FieldType.Keyword)
  private UUID conversationId;

  @Field(type = FieldType.Keyword)
  private UUID senderId;

  @Field(type = FieldType.Text, analyzer = "dm_ngram_analyzer")
  private String content;

  @Field(type = FieldType.Date)
  private Instant createdAt;

  public static DirectMessageDocument from (DirectMessage message) {
    return new DirectMessageDocument(
        message.getId(),
        message.getConversation().getId(),
        message.getSender().getId(),
        message.getContent(),
        message.getCreatedAt()
    );
  }
}
