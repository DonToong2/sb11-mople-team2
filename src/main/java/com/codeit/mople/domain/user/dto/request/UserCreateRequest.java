package com.codeit.mople.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 6,  max = 20, message = "패스워드는 최소 6자, 최대 20자여야 합니다.")
    String password,

    @NotBlank(message = "이름을 입력해주세요.")
    String name
) {}