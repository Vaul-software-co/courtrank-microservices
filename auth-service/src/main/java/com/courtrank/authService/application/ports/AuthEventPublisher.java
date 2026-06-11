package com.courtrank.authService.application.ports;

import com.courtrank.authService.application.events.UserRegisteredEvent;
import com.courtrank.authService.application.events.UserRestoredEvent;
import com.courtrank.authService.application.events.UserDeletedEvent;
import com.courtrank.authService.application.events.UserEmailVerifiedEvent;

public interface AuthEventPublisher {
    void publishUserRegistered(UserRegisteredEvent event);
    void publishUserRestored(UserRestoredEvent event);
    void publishUserDeleted(UserDeletedEvent event);
    void publishUserEmailVerified(UserEmailVerifiedEvent event);
}
