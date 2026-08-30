package com.codeit.mople.domain.user.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoleUpdateRequest(
    @NotBlank(message = "권한은 필수입니다.")
    @Pattern(regexp = "ADMIN|USER", message = "허용된 역할은 ADMIN, USER입니다.")
    String role
) {

}
