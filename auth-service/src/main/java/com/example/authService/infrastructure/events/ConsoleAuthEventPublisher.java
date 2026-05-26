package com.example.authService.infrastructure.events;

import com.example.authService.application.events.UserRegisteredEvent;
import com.example.authService.application.events.UserRestoredEvent;
import com.example.authService.application.ports.AuthEventPublisher;
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
}
