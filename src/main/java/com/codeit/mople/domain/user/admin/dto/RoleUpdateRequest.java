package com.codeit.mople.domain.user.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleUpdateRequest(
    @NotBlank(message = "권한은 필수입니다.")
    String role
) {}
