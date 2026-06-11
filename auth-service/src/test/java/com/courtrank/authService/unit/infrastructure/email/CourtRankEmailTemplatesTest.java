package com.courtrank.authService.unit.infrastructure.email;

import com.courtrank.authService.infrastructure.email.CourtRankEmailTemplates;
import com.courtrank.authService.infrastructure.email.EmailTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CourtRankEmailTemplatesTest {

    @Test
    void verificationEmail_shouldUseSpanishTemplateByDefault() {
        EmailTemplate template = CourtRankEmailTemplates.verificationEmail("https://app.test/verify", "fr");

        assertEquals("Verifica tu cuenta de CourtRank", template.subject());
        assertTrue(template.html().contains("Verifica tu correo"));
        assertTrue(template.html().contains("https://app.test/verify"));
    }

    @Test
    void verificationEmail_shouldUseEnglishTemplateWhenLangIsEn() {
        EmailTemplate template = CourtRankEmailTemplates.verificationEmail("https://app.test/verify", "en");

        assertEquals("Verify your CourtRank account", template.subject());
        assertTrue(template.html().contains("Verify your email"));
        assertTrue(template.html().contains("https://app.test/verify"));
    }

    @Test
    void passwordReset_shouldUseSpanishTemplateByDefault() {
        EmailTemplate template = CourtRankEmailTemplates.passwordReset("123456", null);

        assertEquals("Recupera tu contraseña de CourtRank", template.subject());
        assertTrue(template.html().contains("Recuperación de contraseña"));
        assertTrue(template.html().contains("123456"));
    }

    @Test
    void passwordReset_shouldUseEnglishTemplateWhenLangIsEn() {
        EmailTemplate template = CourtRankEmailTemplates.passwordReset("123456", "en");

        assertEquals("Reset your CourtRank password", template.subject());
        assertTrue(template.html().contains("Password reset"));
        assertTrue(template.html().contains("123456"));
    }
}
