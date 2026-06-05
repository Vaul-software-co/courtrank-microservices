package com.example.authService.application.dto;

import java.time.Instant;

public record UpdateDataConsentResponse(
        Instant acceptedDataCommercializationAt
) {
}
