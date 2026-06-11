package com.courtrank.authService.domain.exceptions;

public class MissedTermsAndConditionsException extends RuntimeException {
    public MissedTermsAndConditionsException() {
        super("Terms and conditions must be accepted");
    }
}
