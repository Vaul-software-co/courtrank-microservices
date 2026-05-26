package com.example.authService.infrastructure.authorization;

import com.example.authService.application.ports.authorization.WorkerAccess;
import com.example.authService.application.ports.authorization.WorkerAccessVerifier;

import java.util.UUID;

public class DenyWorkerAccessVerifier implements WorkerAccessVerifier {
    @Override
    public WorkerAccess verify(UUID userId) {
        return WorkerAccess.denied();
    }
}
