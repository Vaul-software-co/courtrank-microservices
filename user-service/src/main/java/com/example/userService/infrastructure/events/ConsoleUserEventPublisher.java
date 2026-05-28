package com.example.userService.infrastructure.events;

import com.example.userService.application.events.UserProfileCreatedEvent;
import com.example.userService.application.ports.UserEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleUserEventPublisher implements UserEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleUserEventPublisher.class);

    @Override
    public void publishUserProfileCreated(UserProfileCreatedEvent event) {
        logger.info("user_event type=USER_PROFILE_CREATED payload={}", event);
    }
}
