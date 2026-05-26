package com.example.authService.domain.entity;

import com.example.authService.domain.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public class Authentication {
    private UUID id;
    private String email;
    private String passwordHash;
    private UserRole role;

    private boolean isEmailVerified;
    private boolean isActive;

    private String termsVersionAccepted;
    private Instant termsAcceptedAt;

    private Instant dataConsentAcceptedAt;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private Authentication(
            UUID id,
            String email,
            String passwordHash,
            UserRole role,
            boolean isEmailVerified,
            boolean isActive,
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
        this.isEmailVerified = isEmailVerified;
        this.isActive = isActive;
        this.termsVersionAccepted = termsVersionAccepted;
        this.termsAcceptedAt = termsAcceptedAt;
        this.dataConsentAcceptedAt = dataConsentAcceptedAt;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Authentication create(String email, String passwordHash, UserRole role) {
        Instant now = Instant.now();
        return new Authentication(
                UUID.randomUUID(),
                email,
                passwordHash,
                role,
                false,
                true,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public static Authentication restore(
            UUID id,
            String email,
            String passwordHash,
            UserRole role,
            boolean isEmailVerified,
            boolean isActive,
            String termsVersionAccepted,
            Instant termsAcceptedAt,
            Instant dataConsentAcceptedAt,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Authentication(
                id,
                email,
                passwordHash,
                role,
                isEmailVerified,
                isActive,
                termsVersionAccepted,
                termsAcceptedAt,
                dataConsentAcceptedAt,
                deletedAt,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public UserRole getRole() {
        return this.role;
    }

    public boolean isEmailVerified() {
        return this.isEmailVerified;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public String getTermsVersionAccepted(){
        return this.termsVersionAccepted;
    }
    public Instant getTermsAcceptedAt() {
        return this.termsAcceptedAt;
    }

    public Instant getDeletedAt() {
        return this.deletedAt;
    }

    public Instant getDataConsentAcceptedAt() {
        return this.dataConsentAcceptedAt;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt(){
        return  this.updatedAt;
    }

    public void acceptTerms(String version){
        this.termsAcceptedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.termsVersionAccepted = version;
    }

    public void changePassword(String newHash){
        this.passwordHash = newHash;
        this.updatedAt = Instant.now();
    }

    public void verifyEmail(){
        this.isEmailVerified = true;
        this.updatedAt = Instant.now();
    }

    public void acceptData(){
        this.dataConsentAcceptedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isDataAccepted(){
        return this.dataConsentAcceptedAt != null;
    }

    public boolean isDeleted(){
        return this.deletedAt != null;
    }

    public void deleteUser(){
        if(isDeleted()) return;
        Instant now = Instant.now();
        this.isActive = false;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void restoreUser(String newPasswordHash){
        if(!isDeleted()) return;

        Instant now = Instant.now();
        this.passwordHash = newPasswordHash;
        this.isActive = true;
        this.isEmailVerified = false;
        this.deletedAt = null;
        this.updatedAt = now;
    }

}
