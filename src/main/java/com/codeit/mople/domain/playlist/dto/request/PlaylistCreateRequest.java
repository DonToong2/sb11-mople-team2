package com.codeit.mople.domain.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaylistCreateRequest(
    @NotBlank
    String title,
    
    // TODO: schema.sql 확인 후 Bean Validation 적용
    String description
) {

}
