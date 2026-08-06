package com.codeit.mople.domain.content.repository;

import com.codeit.mople.domain.content.entity.Content;
import com.codeit.mople.domain.content.entity.ContentType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

  // 청크 단위 중복 판정용
  @Query("SELECT c.title FROM Content c WHERE c.type = :type and c.title in :titles")
  List<String> findTitleByTypeAndTitleIn(
      @Param("type") ContentType type,
      @Param("titles") Collection<String> titles);
}
