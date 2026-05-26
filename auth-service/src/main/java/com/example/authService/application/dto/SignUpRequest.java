package com.example.authService.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank
        String name,

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
        boolean commercial
) {
    public boolean isTerms() {
        return this.terms;
    }

    public boolean isCommercial() {
        return this.commercial;
    }
}
