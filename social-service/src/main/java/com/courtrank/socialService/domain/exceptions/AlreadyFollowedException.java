package com.courtrank.socialService.domain.exceptions;

public class AlreadyFollowedException extends RuntimeException {
    public AlreadyFollowedException() {
        super("User already followed");
    }
}
