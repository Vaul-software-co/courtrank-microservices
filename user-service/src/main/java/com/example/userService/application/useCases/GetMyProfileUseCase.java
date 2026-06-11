package com.example.userService.application.useCases;

import com.example.userService.application.dto.GetProfileRequest;
import com.example.userService.application.dto.MyProfileResponse;
import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.exceptions.InvalidCredentialsException;
import com.example.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

public class GetMyProfileUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public GetMyProfileUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public MyProfileResponse execute(GetProfileRequest request, TraceContext trace){
        User user = this.userRepository.findById(request.userId())
                            .orElse(null);

        if(user == null){
            this.auditLogger.log(
                    new UserAuditEvent(
                            UserAuditEventType.USER_PROFILE_LOOKUP_FAILED_NOT_FOUND,
                            request.userId(),
                            request.userId(),
                            TraceContext.traceIdOrNull(trace),
                            Map.of(),
                            Instant.now()
                    )
            );
            throw new InvalidCredentialsException();
        }

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
