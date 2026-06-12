package com.courtrank.socialService.domain.exceptions;

public class FollowNotFoundException extends RuntimeException {
    public FollowNotFoundException() {
        super("Follow not found");
    }
}
