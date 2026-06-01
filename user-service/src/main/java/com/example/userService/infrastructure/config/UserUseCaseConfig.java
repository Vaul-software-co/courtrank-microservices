package com.example.userService.infrastructure.config;

import com.example.userService.application.ports.UserEventPublisher;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.application.useCases.CheckUsernameAvailabilityUseCase;
import com.example.userService.application.useCases.CreateUserFromAuthEventUseCase;
import com.example.userService.application.useCases.DeleteUserFromAuthEventUseCase;
import com.example.userService.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserUseCaseConfig {
    @Bean
    public CheckUsernameAvailabilityUseCase checkUsernameAvailabilityUseCase(UserRepository userRepository) {
        return new CheckUsernameAvailabilityUseCase(userRepository);
    }

    @Bean
    public CreateUserFromAuthEventUseCase createUserFromAuthEventUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger,
            UserEventPublisher eventPublisher
    ) {
        return new CreateUserFromAuthEventUseCase(userRepository, auditLogger, eventPublisher);
    }

    @Bean
    public DeleteUserFromAuthEventUseCase deleteUserFromAuthEventUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new DeleteUserFromAuthEventUseCase(userRepository, auditLogger);
    }
}
