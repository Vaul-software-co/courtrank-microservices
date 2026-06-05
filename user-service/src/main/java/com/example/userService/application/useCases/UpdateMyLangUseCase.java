package com.example.userService.application.useCases;

import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.dto.UpdateMyLangRequest;
import com.example.userService.application.dto.UpdateMyLangResponse;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.exceptions.InvalidCredentialsException;
import com.example.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class UpdateMyLangUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public UpdateMyLangUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public UpdateMyLangResponse execute(UpdateMyLangRequest request, TraceContext trace) {
        User user = this.userRepository.findById(request.userId())
                .orElse(null);

        if (user == null) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_LANG_UPDATE_FAILED_NOT_FOUND,
                    request.userId(),
                    request.userId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new InvalidCredentialsException();
        }

        String previousLang = user.getLang();
        if (Objects.equals(previousLang, request.lang())) {
            return new UpdateMyLangResponse(user.getLang());
        }

        user.changeLang(request.lang());
        this.userRepository.save(user);

        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_LANG_UPDATED,
                user.getId(),
                user.getId(),
                TraceContext.traceIdOrNull(trace),
                Map.of(
                        "previousLang", String.valueOf(previousLang),
                        "lang", user.getLang()
                ),
                Instant.now()
        ));

        return new UpdateMyLangResponse(user.getLang());
    }
}
