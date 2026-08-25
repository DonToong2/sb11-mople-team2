package com.codeit.mople.global.event.failure;

import com.codeit.mople.global.config.KafkaProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = KafkaProperties.PREFIX, name = "enabled", havingValue = "true")
@Endpoint(id = "failedevents")
public class FailedEventsEndpoint {

  private static final int DEFAULT_WITHIN_HOURS = 24;
  private static final int DEFAULT_LIMIT = 100;
  private static final int MAX_LIMIT = 1000;

  private final FailedEventStore failedEventStore;
  private final FailedEventReplayer replayer;
  
  public record FailedEventSummary(
      String recordId,
      String topic,
      UUID eventId,
      String eventType,
      String error,
      boolean replayable
  ) {

    static FailedEventSummary from(FailedEvent event) {
      return new FailedEventSummary(
          event.recordId(),
          event.topic(),
          event.eventId(),
          event.eventType(),
          event.error(),
          event.replayable()
      );
    }
  }

  @ReadOperation
  public List<FailedEventSummary> list(
      @Nullable Integer withinHours,
      @Nullable Integer limit
  ) {
    return failedEventStore.findRecent(within(withinHours), limit(limit)).stream()
        .map(FailedEventSummary::from)
        .toList();
  }

  @WriteOperation
  public FailedEventReplayResult replay(
      @Nullable Integer withinHours,
      @Nullable Integer limit
  ) {
    return replayer.replay(within(withinHours), limit(limit));
  }

  private Duration within(Integer withinHours) {
    return Duration.ofHours(withinHours == null ? DEFAULT_WITHIN_HOURS : withinHours);
  }

  private int limit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }

    return Math.min(limit, MAX_LIMIT);
  }
}