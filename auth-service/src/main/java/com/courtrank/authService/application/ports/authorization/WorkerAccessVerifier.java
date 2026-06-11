package com.courtrank.authService.application.ports.authorization;

import java.util.UUID;

public interface WorkerAccessVerifier {
    WorkerAccess verify(UUID userId);
}
