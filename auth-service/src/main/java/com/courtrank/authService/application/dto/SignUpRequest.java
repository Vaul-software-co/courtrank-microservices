package com.courtrank.authService.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        String name,

        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-ZñÑ0-9_]+$", message = "Username can only contain letters, numbers and underscores")
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password,

        @JsonAlias({"isTerms", "acceptedTerms"})
        @AssertTrue(message = "You must accept the terms and conditions")
        boolean terms,

        @JsonAlias("acceptedTermsVersion")
        @NotBlank
        String termsVersion,

        @JsonAlias({"isCommercial", "acceptedDataCommercialization"})
        boolean commercial,

        String lang
) {
    public boolean isTerms() {
        return this.terms;
    }

    public boolean isCommercial() {
        return this.commercial;
    }
}
