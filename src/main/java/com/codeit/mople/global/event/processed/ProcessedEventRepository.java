package com.codeit.mople.global.event.processed;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

  boolean existsByEventId(UUID eventId);

}
