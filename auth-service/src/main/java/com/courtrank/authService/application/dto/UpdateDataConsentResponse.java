package com.courtrank.authService.application.dto;

import java.time.Instant;

public record UpdateDataConsentResponse(
        Instant acceptedDataCommercializationAt
) {
}
