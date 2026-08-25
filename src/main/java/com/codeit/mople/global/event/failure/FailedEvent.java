package com.codeit.mople.global.event.failure;

import com.codeit.mople.global.event.PublishableEvent;
import java.util.UUID;

public record FailedEvent(
    String recordId,
    String topic,
    String key,
    UUID eventId,
    String eventType,
    String data,
    String error
) {

  public static FailedEvent of(
      String topic,
      String key,
      PublishableEvent event,
      String data,
      Throwable cause
  ) {
    return new FailedEvent(
        null,
        topic,
        key == null ? "" : key,
        event.eventId(),
        event.getClass().getName(),
        data == null ? "" : data,
        cause == null ? "" : String.valueOf(cause.getMessage())
    );
  }
  
  public boolean replayable() {
    return !data.isBlank();
  }
  
  public String keyOrNull() {
    return key.isEmpty() ? null : key;
  }
}