package com.codeit.mople.domain.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaylistCreateRequest(
    @NotBlank(message = "제목을 입력해주세요.")
    String title,

    @NotBlank(message = "설명을 입력해주세요.")
    String description
) {

}
