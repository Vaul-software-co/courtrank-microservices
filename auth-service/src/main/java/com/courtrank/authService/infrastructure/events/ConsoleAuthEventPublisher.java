package com.courtrank.authService.infrastructure.events;

import com.courtrank.authService.application.events.UserDeletedEvent;
import com.courtrank.authService.application.events.UserEmailVerifiedEvent;
import com.courtrank.authService.application.events.UserRegisteredEvent;
import com.courtrank.authService.application.events.UserRestoredEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleAuthEventPublisher implements AuthEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleAuthEventPublisher.class);

    @Override
    public void publishUserRegistered(UserRegisteredEvent event) {
        logger.info("auth event user_registered userId={} email={}", event.id(), event.email());
    }

    @Override
    public void publishUserRestored(UserRestoredEvent event) {
        logger.info("auth event user_restored userId={} email={}", event.id(), event.email());
    }

    @Override
    public void publishUserDeleted(UserDeletedEvent event) {
        logger.info("auth event user_deleted userId={} email={}", event.id(), event.email());
    }

    @Override
    public void publishUserEmailVerified(UserEmailVerifiedEvent event) {
        logger.info("auth event user_email_verified userId={} email={}", event.id(), event.email());
    }
}
