package com.courtrank.userService.application.ports;

import com.courtrank.userService.application.events.UserProfileChangedEvent;
import com.courtrank.userService.application.events.UserProfileCreatedEvent;
import com.courtrank.userService.application.events.UserProfileDeletedEvent;

public class NoOpUserEventPublisher implements UserEventPublisher {
    @Override public void publishUserProfileCreated(UserProfileCreatedEvent event) {}
    @Override public void publishUserProfileUpdated(UserProfileChangedEvent event) {}
    @Override public void publishUserProfileDeleted(UserProfileDeletedEvent event) {}
    @Override public void publishUserProfileRestored(UserProfileChangedEvent event) {}
    @Override public void publishUserProfileBecamePublic(UserProfileChangedEvent event) {}
}
