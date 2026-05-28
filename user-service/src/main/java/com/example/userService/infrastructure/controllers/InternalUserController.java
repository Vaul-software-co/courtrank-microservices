package com.example.userService.infrastructure.controllers;

import com.example.userService.application.dto.UsernameAvailabilityResponse;
import com.example.userService.application.useCases.CheckUsernameAvailabilityUseCase;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
@Validated
public class InternalUserController {
    private static final String INTERNAL_API_KEY_HEADER = "x-internal-api-key";

    private final CheckUsernameAvailabilityUseCase checkUsernameAvailabilityUseCase;
    private final String internalApiKey;

    public InternalUserController(
            CheckUsernameAvailabilityUseCase checkUsernameAvailabilityUseCase,
            @Value("${app.internal-api-key}") String internalApiKey
    ) {
        this.checkUsernameAvailabilityUseCase = checkUsernameAvailabilityUseCase;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/username-available")
    public UsernameAvailabilityResponse usernameAvailable(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String apiKey,
            @RequestParam
            @Size(min = 3, max = 30)
            @Pattern(regexp = "^[a-zA-ZñÑ0-9_]+$", message = "Username can only contain letters, numbers and underscores")
            String username,
            @RequestParam UUID userId
    ) {
        this.verifyInternalApiKey(apiKey);

        return new UsernameAvailabilityResponse(
                this.checkUsernameAvailabilityUseCase.execute(username, userId)
        );
    }

    private void verifyInternalApiKey(String apiKey) {
        if (!this.internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }
}
