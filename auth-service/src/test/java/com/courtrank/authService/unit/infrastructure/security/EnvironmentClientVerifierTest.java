package com.courtrank.authService.unit.infrastructure.security;

import com.courtrank.authService.application.ports.security.ApiClient;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.infrastructure.security.EnvironmentClientVerifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnvironmentClientVerifierTest {
    private static final String WEB_API_KEY = "web-key";
    private static final String MOBILE_API_KEY = "mobile-key";

    private EnvironmentClientVerifier createVerifier() {
        return new EnvironmentClientVerifier(WEB_API_KEY, MOBILE_API_KEY);
    }

    @Test
    void verify_shouldReturnWebClientWhenApiKeyMatchesWebKey() {
        EnvironmentClientVerifier verifier = this.createVerifier();

        ApiClient client = verifier.verify(WEB_API_KEY);

        assertEquals("web", client.client());
        assertEquals(UserRole.ADMIN, client.type());
    }

    @Test
    void verify_shouldReturnMobileClientWhenApiKeyMatchesMobileKey() {
        EnvironmentClientVerifier verifier = this.createVerifier();

        ApiClient client = verifier.verify(MOBILE_API_KEY);

        assertEquals("mobile", client.client());
        assertEquals(UserRole.MEMBER, client.type());
    }

    @Test
    void verify_shouldThrowInvalidCredentialsWhenApiKeyIsInvalid() {
        EnvironmentClientVerifier verifier = this.createVerifier();

        assertThrows(
                InvalidCredentialsException.class,
                () -> verifier.verify("invalid-key")
        );
    }

    @Test
    void verify_shouldThrowInvalidCredentialsWhenApiKeyIsMissing() {
        EnvironmentClientVerifier verifier = this.createVerifier();

        assertThrows(
                InvalidCredentialsException.class,
                () -> verifier.verify(null)
        );
    }
}
