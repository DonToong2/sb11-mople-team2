package com.codeit.mople.domain.review.dto.response;

import com.codeit.mople.global.dto.UserSummary;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID contentId,
    UserSummary author,
    String text,
    double rating
) {

}
