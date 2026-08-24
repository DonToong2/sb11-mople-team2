package com.codeit.mople.global.event.failure;

public interface FailedEventStore {

  void save(FailedEvent event);

}
