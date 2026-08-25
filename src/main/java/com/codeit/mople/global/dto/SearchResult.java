package com.codeit.mople.global.dto;

import java.util.List;
import java.util.UUID;

public record SearchResult(
    List<UUID> ids,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount
) {

}
