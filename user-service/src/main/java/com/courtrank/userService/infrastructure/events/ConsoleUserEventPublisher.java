package com.courtrank.userService.infrastructure.events;

import com.courtrank.userService.application.events.UserProfileChangedEvent;
import com.courtrank.userService.application.events.UserProfileCreatedEvent;
import com.courtrank.userService.application.events.UserProfileDeletedEvent;
import com.courtrank.userService.application.ports.UserEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleUserEventPublisher implements UserEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleUserEventPublisher.class);

    @Override
    public void publishUserProfileCreated(UserProfileCreatedEvent event) {
        logger.info("user_event type=USER_PROFILE_CREATED payload={}", event);
    }

    @Override
    public void publishUserProfileUpdated(UserProfileChangedEvent event) {
        logger.info("user_event type=USER_PROFILE_UPDATED payload={}", event);
    }

    @Override
    public void publishUserProfileDeleted(UserProfileDeletedEvent event) {
        logger.info("user_event type=USER_PROFILE_DELETED payload={}", event);
    }

    @Override
    public void publishUserProfileRestored(UserProfileChangedEvent event) {
        logger.info("user_event type=USER_PROFILE_RESTORED payload={}", event);
    }

    @Override
    public void publishUserProfileBecamePublic(UserProfileChangedEvent event) {
        logger.info("user_event type=USER_PROFILE_BECAME_PUBLIC payload={}", event);
    }
}
