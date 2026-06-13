package com.courtrank.authService.infrastructure.controllers;

import com.courtrank.authService.application.dto.ChangePasswordRequest;
import com.courtrank.authService.application.dto.DeleteUserRequest;
import com.courtrank.authService.application.dto.UpdateDataConsentRequest;
import com.courtrank.authService.application.dto.UpdateDataConsentResponse;
import com.courtrank.authService.application.useCases.ChangePasswordUseCase;
import com.courtrank.authService.application.useCases.DeleteUserUseCase;
import com.courtrank.authService.application.useCases.UpdateDataConsentUseCase;
import com.courtrank.authService.infrastructure.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class LegacyUserAuthController {
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UpdateDataConsentUseCase updateDataConsentUseCase;

    public LegacyUserAuthController(
            ChangePasswordUseCase changePasswordUseCase,
            DeleteUserUseCase deleteUserUseCase,
            UpdateDataConsentUseCase updateDataConsentUseCase
    ) {
        this.changePasswordUseCase = changePasswordUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.updateDataConsentUseCase = updateDataConsentUseCase;
    }

    @PutMapping("/me/password")
    public Map<String, String> changePassword(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody ChangePasswordBody body
    ) {
        this.changePasswordUseCase.execute(new ChangePasswordRequest(
                principal.userId(),
                body.oldPassword(),
                body.newPassword()
        ));

        return Map.of("message", "Password updated successfully");
    }

    @DeleteMapping("/me")
    public Map<String, String> deleteMe(@AuthenticationPrincipal AuthUserPrincipal principal) {
        this.deleteUserUseCase.execute(new DeleteUserRequest(principal.userId()));
        return Map.of("message", "User deleted");
    }

    @PostMapping("/me/data-consent")
    public UpdateDataConsentResponse updateDataConsent(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody DataConsentBody body
    ) {
        return this.updateDataConsentUseCase.execute(new UpdateDataConsentRequest(
                principal.userId(),
                body.accept()
        ));
    }

    public record ChangePasswordBody(
            @NotBlank
            String oldPassword,

            @NotBlank
            String newPassword
    ) {
    }

    public record DataConsentBody(
            boolean accept
    ) {
    }
}
