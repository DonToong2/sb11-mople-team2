package com.codeit.mople.domain.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewCreateRequest(
    @NotNull(message = "컨텐츠 ID는 필수입니다.")
    UUID contentId,

    @NotBlank(message = "리뷰 내용을 작성해주세요.")
    String text,

    @DecimalMin(value = "1.0")
    @DecimalMax(value = "5.0")
    @NotNull(message = "별점을 선택해주세요.")
    Double rating
) {

}
