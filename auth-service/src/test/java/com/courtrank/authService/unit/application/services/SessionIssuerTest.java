package com.courtrank.authService.unit.application.services;

import com.courtrank.authService.application.dto.AuthResponse;
import com.courtrank.authService.application.dto.HttpContext;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.services.SessionIssuer;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.enums.TokenType;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SessionIssuerTest {
    @Mock
    SessionRepository sessionRepository;

    @Mock
    TokenService tokenService;

    @Mock
    TokenHasher tokenHasher;

    @InjectMocks
    SessionIssuer sessionIssuer;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String REFRESH_TOKEN_HASH = "refresh-token-hash";

    @Test
    void issue_shouldCreateSessionAndReturnTokens() {
        Authentication auth = Authentication.create(EMAIL, PASSWORD_HASH, UserRole.MEMBER);
        HttpContext http = new HttpContext("mobile", "127.0.0.1", "Safari", UserRole.MEMBER);

        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(REFRESH_TOKEN);
        when(this.tokenHasher.hash(REFRESH_TOKEN))
                .thenReturn(REFRESH_TOKEN_HASH);
        when(this.tokenService.generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole())))
                .thenReturn(ACCESS_TOKEN);

        AuthResponse response = this.sessionIssuer.issue(auth, http);

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(this.sessionRepository).save(sessionCaptor.capture());

        Session savedSession = sessionCaptor.getValue();
        assertEquals(ACCESS_TOKEN, response.accessToken());
        assertEquals(REFRESH_TOKEN, response.refreshToken());
        assertTrue(response.clubId().isEmpty());
        assertEquals(auth.getId(), savedSession.getUserId());
        assertEquals(REFRESH_TOKEN_HASH, savedSession.getRefreshTokenHash());
        assertEquals(http.client(), savedSession.getClient());
        assertEquals(http.ip(), savedSession.getIp());
        assertEquals(http.userAgent(), savedSession.getUserAgent());
        assertTrue(savedSession.isActive());
    }
}
