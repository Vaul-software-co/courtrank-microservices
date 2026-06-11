package com.courtrank.authService.infrastructure.persistence.jpa.entity;

import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authentications")
public class AuthenticationJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "terms_version_accepted")
    private String termsVersionAccepted;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "data_consent_accepted_at")
    private Instant dataConsentAcceptedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuthenticationJpaEntity() {
    }

    public AuthenticationJpaEntity(
            UUID id,
            String email,
            String passwordHash,
            UserRole role,
            boolean emailVerified,
            boolean active,
            String termsVersionAccepted,
            Instant termsAcceptedAt,
            Instant dataConsentAcceptedAt,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailVerified = emailVerified;
        this.active = active;
        this.termsVersionAccepted = termsVersionAccepted;
        this.termsAcceptedAt = termsAcceptedAt;
        this.dataConsentAcceptedAt = dataConsentAcceptedAt;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AuthenticationJpaEntity fromDomain(Authentication authentication) {
        return new AuthenticationJpaEntity(
                authentication.getId(),
                authentication.getEmail(),
                authentication.getPasswordHash(),
                authentication.getRole(),
                authentication.isEmailVerified(),
                authentication.isActive(),
                authentication.getTermsVersionAccepted(),
                authentication.getTermsAcceptedAt(),
                authentication.getDataConsentAcceptedAt(),
                authentication.getDeletedAt(),
                authentication.getCreatedAt(),
                authentication.getUpdatedAt()
        );
    }

    public Authentication toDomain() {
        return Authentication.restore(
                this.id,
                this.email,
                this.passwordHash,
                this.role,
                this.emailVerified,
                this.active,
                this.termsVersionAccepted,
                this.termsAcceptedAt,
                this.dataConsentAcceptedAt,
                this.deletedAt,
                this.createdAt,
                this.updatedAt
        );
    }
}
