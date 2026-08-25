package com.codeit.mople.global.event.failure;

public record FailedEventReplayResult(
    int total,
    int replayed,
    int skipped,
    int failed
) {

}