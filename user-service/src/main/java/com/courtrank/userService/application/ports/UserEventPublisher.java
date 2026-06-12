package com.courtrank.userService.application.ports;

import com.courtrank.userService.application.events.UserProfileCreatedEvent;
import com.courtrank.userService.application.events.UserProfileChangedEvent;
import com.courtrank.userService.application.events.UserProfileDeletedEvent;

public interface UserEventPublisher {
    void publishUserProfileCreated(UserProfileCreatedEvent event);
    void publishUserProfileUpdated(UserProfileChangedEvent event);
    void publishUserProfileDeleted(UserProfileDeletedEvent event);
    void publishUserProfileRestored(UserProfileChangedEvent event);
    void publishUserProfileBecamePublic(UserProfileChangedEvent event);
}
