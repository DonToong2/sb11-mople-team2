package com.codeit.mople.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank
    @Size(min = 0, max = 20)
    String password
) {}