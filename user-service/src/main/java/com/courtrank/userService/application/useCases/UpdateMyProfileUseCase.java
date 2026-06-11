package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.MyProfileResponse;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UpdateMyProfileRequest;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.userService.domain.exceptions.UserNameAlreadyTakenException;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateMyProfileUseCase {

    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public UpdateMyProfileUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public MyProfileResponse execute(UpdateMyProfileRequest request, TraceContext trace){
        User user = this.userRepository.findById(request.userId())
                .orElse(null);

        if(user == null){
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_UPDATE_FAILED_NOT_FOUND,
                    request.userId(),
                    request.userId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new InvalidCredentialsException();
        }

        List<String> updatedFields = new ArrayList<>();

        if (request.name() != null) {
            user.changeName(request.name());
            updatedFields.add("name");
        }

        if (request.username() != null && !request.username().equals(user.getUserName())) {
            User usernameOwner = this.userRepository.findByUsername(request.username())
                    .orElse(null);

            if (usernameOwner != null && !usernameOwner.getId().equals(user.getId())) {
                this.auditLogger.log(new UserAuditEvent(
                        UserAuditEventType.USER_PROFILE_UPDATE_FAILED_USERNAME_CONFLICT,
                        user.getId(),
                        user.getId(),
                        TraceContext.traceIdOrNull(trace),
                        Map.of(
                                "username", request.username(),
                                "conflictingUserId", usernameOwner.getId().toString()
                        ),
                        Instant.now()
                ));
                throw new UserNameAlreadyTakenException();
            }

            user.changeUsername(request.username());
            updatedFields.add("username");
        }

        if (request.phoneNumber() != null) {
            user.changePhoneNumber(request.phoneNumber());
            updatedFields.add("phoneNumber");
        }

        if (request.gender() != null) {
            user.changeGender(request.gender());
            updatedFields.add("gender");
        }

        this.userRepository.save(user);

        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_UPDATED,
                user.getId(),
                user.getId(),
                TraceContext.traceIdOrNull(trace),
                Map.of("updatedFields", updatedFields),
                Instant.now()
        ));

        return this.toResponse(user);
    }

    private MyProfileResponse toResponse(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getName(),
                user.getUserName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getAvatarUrl(),
                user.isPrivateProfile(),
                user.getStatus(),
                user.getLang(),
                user.getCreatedAt()
        );
    }

}
