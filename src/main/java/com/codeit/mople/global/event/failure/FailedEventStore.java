package com.codeit.mople.global.event.failure;

import java.util.List;

public interface FailedEventStore {

  void save(FailedEvent event);

  List<FailedEvent> find(FailedEventQuery query);

  void delete(String recordId);

}