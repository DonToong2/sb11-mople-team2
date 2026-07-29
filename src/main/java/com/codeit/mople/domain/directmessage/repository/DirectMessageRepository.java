package com.codeit.mople.domain.directmessage.repository;

import com.codeit.mople.domain.directmessage.entity.DirectMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

  @Query("SELECT dm FROM DirectMessage dm "
  + "JOIN FETCH dm.sender "
  + "JOIN FETCH dm.receiver "
  + "WHERE dm.conversation.id = :conversationId "
  + "ORDER BY dm.createdAt DESC ")
  List<DirectMessage> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") UUID conversationId);
}
