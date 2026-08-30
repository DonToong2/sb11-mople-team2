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

    //null 입력 시 NullPointException 방지
    if (value == null) {
      throw new IllegalArgumentException("지원하지 않는 ContentType: null");
    }

    String normalizedInput = value.replace("_", "").replace("-", "").replaceAll("\\s+", "")
        .toLowerCase();

    //프론트엔드가 sports로 보내도 sport로 인식하도록 처리
    if ("sports".equals(normalizedInput)) {
      normalizedInput = "sport";
    }

    String finalInput = normalizedInput;

    return Arrays.stream(values())
        .filter(type -> type.value.toLowerCase().equals(finalInput)
            || type.name().toLowerCase().equals(finalInput))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 ContentType: " + value));
  }
}
