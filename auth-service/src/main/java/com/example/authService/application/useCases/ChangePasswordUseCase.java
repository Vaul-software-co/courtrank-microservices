package com.example.authService.application.useCases;

import com.example.authService.application.dto.ChangePasswordRequest;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.service.PasswordPolicy;

import java.time.Instant;
import java.util.Map;

public class ChangePasswordUseCase {
    private final AuthenticationRepository authenticationRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final AuditLogger auditLogger;

    public ChangePasswordUseCase(AuthenticationRepository authenticationRepository, PasswordHasher passwordHasher, PasswordPolicy passwordPolicy, AuditLogger auditLogger){
        this.authenticationRepository = authenticationRepository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.auditLogger = auditLogger;
    }

    public void execute(ChangePasswordRequest request){
        Authentication auth = this.authenticationRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new InvalidCredentialsException();

        boolean isPasswordCorrect = this.passwordHasher.checkPassword(request.oldPassword(), auth.getPasswordHash());

        if(!isPasswordCorrect) throw new InvalidCredentialsException();

        this.passwordPolicy.validate(request.newPassword());
        String newPasswordHash = this.passwordHasher.hashPassword(request.newPassword());

        auth.changePassword(newPasswordHash);

        this.authenticationRepository.save(auth);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_PASSWORD_CHANGED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of(),
                Instant.now()
        ));

    }
}
