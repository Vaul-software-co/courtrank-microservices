package com.courtrank.socialService.domain.exceptions;

public class SocialUserNotFoundException extends RuntimeException {
    public SocialUserNotFoundException() {
        super("Social user not found");
    }
}
