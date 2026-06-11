package com.courtrank.authService.application.ports.authorization;

import java.util.UUID;


public record WorkerAccess(
        boolean hasAccess,
        UUID defaultClubId
){
    public static WorkerAccess denied() {
        return new WorkerAccess(false, null);
    }
}
