package com.courtrank.userService.application.ports;

import com.courtrank.userService.application.events.UserProfileCreatedEvent;

public interface UserEventPublisher {
    void publishUserProfileCreated(UserProfileCreatedEvent event);
}
