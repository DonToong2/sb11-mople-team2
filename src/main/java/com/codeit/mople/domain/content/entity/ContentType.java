package com.codeit.mople.domain.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ContentType {
  MOVIE("movie"),
  TV_SERIES("tvSeries"),
  SPORT("sport");

  private final String value;

  ContentType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ContentType from(String value) {
    //null 또는 빈 값 방어 처리
    if (value == null || value.isBlank()) {
      return null;
    }

    String normalizedInput = value.replace("_", "").replace("-", "").replaceAll("\\s+", "").toLowerCase();

    return Arrays.stream(values())
        .filter(type -> type.value.replace("_", "").toLowerCase().equals(normalizedInput)
            || type.name().replace("_", "").toLowerCase().equals(normalizedInput))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 ContentType: " + value));
  }
}
