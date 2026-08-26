package com.codeit.mople.global.event.failure;

import java.time.Duration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public record FailedEventQuery(
    Duration within,
    String topic,
    int limit,
    Order order
) {

  public enum Order {
    OLDEST_FIRST,
    NEWEST_FIRST
  }

  public FailedEventQuery {
    Assert.notNull(within, "조회 기간은 필수입니다.");
    Assert.isTrue(!within.isZero() && !within.isNegative(), "조회 기간은 0보다 커야 합니다.");
    Assert.isTrue(limit > 0, "조회 건수는 0보다 커야 합니다.");
    Assert.notNull(order, "정렬 순서는 필수입니다.");
  }

  public static FailedEventQuery newestFirst(Duration within, String topic, int limit) {
    return new FailedEventQuery(within, topic, limit, Order.NEWEST_FIRST);
  }

  public static FailedEventQuery oldestFirst(Duration within, String topic, int limit) {
    return new FailedEventQuery(within, topic, limit, Order.OLDEST_FIRST);
  }

  public boolean hasTopicFilter() {
    return StringUtils.hasText(topic);
  }

  public boolean matches(FailedEvent event) {
    return !hasTopicFilter() || topic.equals(event.topic());
  }
}