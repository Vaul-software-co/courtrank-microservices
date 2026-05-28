package com.example.userService.application.ports;

import com.example.userService.application.events.UserProfileCreatedEvent;

public interface UserEventPublisher {
    void publishUserProfileCreated(UserProfileCreatedEvent event);
}
