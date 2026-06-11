package com.courtrank.userService.infrastructure.config;

import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.AssertUserActiveUseCase;
import com.courtrank.userService.application.useCases.BanUserProfileUseCase;
import com.courtrank.userService.application.useCases.CheckUsernameAvailabilityUseCase;
import com.courtrank.userService.application.useCases.CreateUserFromAuthEventUseCase;
import com.courtrank.userService.application.useCases.DeleteUserFromAuthEventUseCase;
import com.courtrank.userService.application.useCases.GetInternalUserSummaryUseCase;
import com.courtrank.userService.application.useCases.GetInternalUsersByIdsUseCase;
import com.courtrank.userService.application.useCases.GetMyProfileUseCase;
import com.courtrank.userService.application.useCases.GetUserPublicProfileUseCase;
import com.courtrank.userService.application.useCases.MarkUserEmailVerifiedFromAuthEventUseCase;
import com.courtrank.userService.application.useCases.RemoveMyAvatarUseCase;
import com.courtrank.userService.application.useCases.RestoreUserFromAuthEventUseCase;
import com.courtrank.userService.application.useCases.SearchUsersUseCase;
import com.courtrank.userService.application.useCases.UnbanUserProfileUseCase;
import com.courtrank.userService.application.useCases.UpdateMyAvatarUseCase;
import com.courtrank.userService.application.useCases.UpdateMyLangUseCase;
import com.courtrank.userService.application.useCases.UpdateMyProfileUseCase;
import com.courtrank.userService.application.useCases.UpdateMyPrivacyUseCase;
import com.courtrank.userService.domain.repository.UserRepository;
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

    @Bean
    public RestoreUserFromAuthEventUseCase restoreUserFromAuthEventUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new RestoreUserFromAuthEventUseCase(userRepository, auditLogger);
    }

    @Bean
    public MarkUserEmailVerifiedFromAuthEventUseCase markUserEmailVerifiedFromAuthEventUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new MarkUserEmailVerifiedFromAuthEventUseCase(userRepository, auditLogger);
    }

    @Bean
    public GetMyProfileUseCase getMyProfileUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new GetMyProfileUseCase(userRepository, auditLogger);
    }

    @Bean
    public UpdateMyProfileUseCase updateMyProfileUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new UpdateMyProfileUseCase(userRepository, auditLogger);
    }

    @Bean
    public UpdateMyPrivacyUseCase updateMyPrivacyUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new UpdateMyPrivacyUseCase(userRepository, auditLogger);
    }

    @Bean
    public UpdateMyLangUseCase updateMyLangUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new UpdateMyLangUseCase(userRepository, auditLogger);
    }

    @Bean
    public GetUserPublicProfileUseCase getUserPublicProfileUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new GetUserPublicProfileUseCase(userRepository, auditLogger);
    }

    @Bean
    public SearchUsersUseCase searchUsersUseCase(UserRepository userRepository) {
        return new SearchUsersUseCase(userRepository);
    }

    @Bean
    public BanUserProfileUseCase banUserProfileUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new BanUserProfileUseCase(userRepository, auditLogger);
    }

    @Bean
    public UnbanUserProfileUseCase unbanUserProfileUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new UnbanUserProfileUseCase(userRepository, auditLogger);
    }

    @Bean
    public GetInternalUserSummaryUseCase getInternalUserSummaryUseCase(UserRepository userRepository) {
        return new GetInternalUserSummaryUseCase(userRepository);
    }

    @Bean
    public GetInternalUsersByIdsUseCase getInternalUsersByIdsUseCase(UserRepository userRepository) {
        return new GetInternalUsersByIdsUseCase(userRepository);
    }

    @Bean
    public AssertUserActiveUseCase assertUserActiveUseCase(UserRepository userRepository) {
        return new AssertUserActiveUseCase(userRepository);
    }

    @Bean
    public UpdateMyAvatarUseCase updateMyAvatarUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new UpdateMyAvatarUseCase(userRepository, auditLogger);
    }

    @Bean
    public RemoveMyAvatarUseCase removeMyAvatarUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger
    ) {
        return new RemoveMyAvatarUseCase(userRepository, auditLogger);
    }
}
