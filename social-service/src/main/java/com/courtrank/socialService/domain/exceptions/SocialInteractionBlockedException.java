package com.courtrank.socialService.domain.exceptions;

public class SocialInteractionBlockedException extends RuntimeException {
    public SocialInteractionBlockedException() {
        super("Social interaction is blocked");
    }
}
