package com.codeit.mople.domain.playlist.dto.request;

import jakarta.validation.constraints.Size;

public record PlaylistUpdateRequest(

    @Size(max = 200, message = "플레이리스트 제목은 200자 이하여야 합니다.")
    String title,

    @Size(max = 200, message = "플레이리스트 설명은 200자 이하여야 합니다.")
    String description
) {

}
