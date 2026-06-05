package com.example.userService.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record UpdateMyLangRequest(
        @NotNull
        UUID userId,

        @NotBlank
        @Pattern(regexp = "^(es|en)$", message = "Language must be es or en")
        String lang
) {
}
