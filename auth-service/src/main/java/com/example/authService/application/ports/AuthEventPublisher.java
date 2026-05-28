package com.example.authService.application.ports;

import com.example.authService.application.events.UserRegisteredEvent;
import com.example.authService.application.events.UserRestoredEvent;
import com.example.authService.application.events.UserDeletedEvent;

public interface AuthEventPublisher {
    void publishUserRegistered(UserRegisteredEvent event);
    void publishUserRestored(UserRestoredEvent event);
    void publishUserDeleted(UserDeletedEvent event);
}
