package com.codeit.mople.global.event.failure;

import java.time.Duration;
import java.util.List;

public interface FailedEventStore {

  void save(FailedEvent event);

  List<FailedEvent> findRecent(Duration within, int limit);

  void delete(String recordId);

}