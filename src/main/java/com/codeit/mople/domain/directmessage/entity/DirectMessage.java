package com.codeit.mople.domain.directmessage.entity;

import com.codeit.mople.domain.conversation.entity.Conversation;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "direct_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectMessage extends BaseTimeEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  @Column(nullable = false, length = 1000)
  private String content;

  // 프론트엔드 대화방 목록의 '새 메시지(빨간 점)' UI 노출 여부를 결정하는 필수 상태값
  // 추후 프론트엔드 고도화 시 '안 읽은 메시지 개수(숫자 배지)' 카운팅 로직으로 확장 가능
  @Column(nullable = false)
  private boolean isRead = false;

  public static DirectMessage createMessage(Conversation conversation, User sender, User receiver, String content) {
    DirectMessage message = new DirectMessage();
    message.conversation = conversation;
    message.sender = sender;
    message.receiver = receiver;
    message.content = content;
    message.isRead = false;
    return message;
  }

  public void markAsRead() {
    this.isRead = true;
  }
}
