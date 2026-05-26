package com.example.authService.application.ports;

import com.example.authService.application.events.UserRegisteredEvent;
import com.example.authService.application.events.UserRestoredEvent;

public interface AuthEventPublisher {
    void publishUserRegistered(UserRegisteredEvent event);
    void publishUserRestored(UserRestoredEvent event);
}
