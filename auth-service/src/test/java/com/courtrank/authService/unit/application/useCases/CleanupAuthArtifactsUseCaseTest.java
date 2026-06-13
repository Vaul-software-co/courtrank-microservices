package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.AuthCleanupResult;
import com.courtrank.authService.application.useCases.CleanupAuthArtifactsUseCase;
import com.courtrank.authService.domain.repository.SessionRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CleanupAuthArtifactsUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-13T08:00:00Z");

    @Mock
    SessionRepository sessionRepository;

    @Mock
    VerificationTokenRepository verificationTokenRepository;

    @Test
    void execute_shouldDeleteOldInactiveSessionsAndConsumedVerificationTokens() {
        CleanupAuthArtifactsUseCase useCase = new CleanupAuthArtifactsUseCase(
                this.sessionRepository,
                this.verificationTokenRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        when(this.sessionRepository.deleteInactiveBefore(NOW.minus(Duration.ofDays(30))))
                .thenReturn(3);
        when(this.verificationTokenRepository.deleteConsumedBefore(NOW.minus(Duration.ofDays(7))))
                .thenReturn(5);

        AuthCleanupResult result = useCase.execute(Duration.ofDays(30), Duration.ofDays(7));

        ArgumentCaptor<Instant> sessionCutoff = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> tokenCutoff = ArgumentCaptor.forClass(Instant.class);
        verify(this.sessionRepository).deleteInactiveBefore(sessionCutoff.capture());
        verify(this.verificationTokenRepository).deleteConsumedBefore(tokenCutoff.capture());

        assertEquals(NOW.minus(Duration.ofDays(30)), sessionCutoff.getValue());
        assertEquals(NOW.minus(Duration.ofDays(7)), tokenCutoff.getValue());
        assertEquals(3, result.deletedSessions());
        assertEquals(5, result.deletedVerificationTokens());
    }
}
