package com.courtrank.authService.unit.domain.entity;

import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationTest {
    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final UserRole ROLE = UserRole.MEMBER;

    private Authentication createAuth() {
        return Authentication.create(
                EMAIL,
                PASSWORD_HASH,
                ROLE
        );
    }

    @Test
    void create_shouldCreateActiveUserWithUnverifiedEmail(){
        Authentication auth = this.createAuth();

        assertNotNull(auth.getId());
        assertEquals(EMAIL, auth.getEmail());
        assertEquals(PASSWORD_HASH, auth.getPasswordHash());
        assertEquals(ROLE, auth.getRole());
        assertTrue(auth.isActive());
        assertFalse(auth.isEmailVerified());
        assertFalse(auth.isDeleted());
        assertNotNull(auth.getCreatedAt());
        assertNotNull(auth.getUpdatedAt());
    }

    @Test
    void acceptTerms_shouldStoreTermsVersionAndAcceptedAt(){
        Authentication auth = this.createAuth();

        auth.acceptTerms("v1");
        assertEquals("v1", auth.getTermsVersionAccepted());
        assertNotNull(auth.getTermsAcceptedAt());
    }

    @Test
    void acceptData_shouldEnableCommercialDataConsent(){
        Authentication auth = this.createAuth();

        auth.acceptData();
        assertTrue(auth.isDataAccepted());
    }

    @Test
    void verifyEmail_shouldMarkEmailAsVerified(){
        Authentication auth = this.createAuth();

        auth.verifyEmail();
        assertTrue(auth.isEmailVerified());
    }

    @Test
    void changePassword_shouldReplacePasswordHash(){
        Authentication auth = this.createAuth();

        auth.changePassword("newHash");
        assertEquals("newHash", auth.getPasswordHash());
    }

    @Test
    void deleteUser_shouldSoftDeleteUser(){
        Authentication auth = this.createAuth();

        auth.deleteUser();
        assertTrue(auth.isDeleted());
        assertFalse(auth.isActive());
        assertNotNull(auth.getDeletedAt());
    }

    @Test
    void deleteUser_shouldBeIdempotentWhenAlreadyDeleted(){
        Authentication auth = this.createAuth();

        auth.deleteUser();
        Instant deletedAt = auth.getDeletedAt();
        auth.deleteUser();
        assertEquals(deletedAt, auth.getDeletedAt());
    }

    @Test
    void restoreUser_shouldReactivateDeletedUserAndReplacePassword(){
        Authentication auth = this.createAuth();

        String newPasswordHash = "newHash";

        auth.deleteUser();
        auth.restoreUser(newPasswordHash);
        assertFalse(auth.isDeleted());
        assertNull(auth.getDeletedAt());
        assertTrue(auth.isActive());
        assertEquals(newPasswordHash, auth.getPasswordHash());
    }

    @Test
    void restoreUser_shouldDoNothingWhenUserIsNotDeleted(){
        Authentication auth = this.createAuth();

        String newPasswordHash = "newHash";

        auth.restoreUser(newPasswordHash);
        assertEquals(EMAIL, auth.getEmail());
        assertNotEquals(newPasswordHash, auth.getPasswordHash());
    }

    @Test
    void restore_shouldRehydrateAuthenticationWithPersistedState(){
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        Authentication auth = Authentication.restore(
                id,
                EMAIL,
                PASSWORD_HASH,
                ROLE,
                true,
                false,
                "v1",
                now,
                now,
                now,
                now,
                now
        );

        assertEquals(id, auth.getId());
        assertEquals(EMAIL, auth.getEmail());
        assertEquals(PASSWORD_HASH, auth.getPasswordHash());
        assertEquals(ROLE, auth.getRole());
        assertTrue(auth.isEmailVerified());
        assertFalse(auth.isActive());
        assertEquals("v1", auth.getTermsVersionAccepted());
        assertEquals(now, auth.getTermsAcceptedAt());
        assertTrue(auth.isDataAccepted());
        assertTrue(auth.isDeleted());
        assertEquals(now, auth.getDeletedAt());
        assertEquals(now, auth.getCreatedAt());
        assertEquals(now, auth.getUpdatedAt());
    }
}
