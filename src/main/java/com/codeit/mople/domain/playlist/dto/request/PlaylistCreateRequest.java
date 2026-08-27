package com.codeit.mople.domain.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "플레이리스트 제목은 200자 이하여야 합니다.")
    String title,

    @NotBlank(message = "설명을 입력해주세요.")
    @Size(max = 200, message = "플레이리스트 설명은 200자 이하여야 합니다.")
    String description
) {

}
