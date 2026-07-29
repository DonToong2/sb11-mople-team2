package com.codeit.mople.domain.user.admin.dto;

import jakarta.validation.constraints.NotNull;

public record LockUpdateRequest(
    @NotNull Boolean locked
) {}
