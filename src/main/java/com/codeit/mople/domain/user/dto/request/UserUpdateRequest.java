package com.codeit.mople.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record UserUpdateRequest (
    @Size(min = 1, max = 20)
    String name,
    MultipartFile profileImage
) {}