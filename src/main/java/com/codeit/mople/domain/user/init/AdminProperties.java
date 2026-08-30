package com.codeit.mople.domain.user.init;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(
    @NotBlank(message = "admin.email은 필수입니다.")
    @Email(message = "admin.email은 올바른 이메일 형식이어야 합니다.")
    String email,
    @NotBlank(message = "admin.password는 필수입니다.")
    String password,
    @NotBlank(message = "admin.name은 필수입니다.")
    String name
) {

}
