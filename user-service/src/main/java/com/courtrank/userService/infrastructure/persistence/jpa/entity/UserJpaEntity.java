package com.courtrank.userService.infrastructure.persistence.jpa.entity;

import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserGender;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "username")
    private String userName;

    @Column(nullable = false)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    private UserGender gender;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "username_changed_at")
    private Instant usernameChangedAt;

    @Column(name = "username_prev_changed_at")
    private Instant usernamePrevChangedAt;

    private String lang;

    @Column(name = "private_profile", nullable = false)
    private boolean privateProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserProfileStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(
            UUID id,
            String name,
            String userName,
            String email,
            boolean emailVerified,
            UserGender gender,
            String phoneNumber,
            String avatarUrl,
            Instant usernameChangedAt,
            Instant usernamePrevChangedAt,
            String lang,
            boolean privateProfile,
            UserProfileStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.userName = userName;
        this.email = email;
        this.emailVerified = emailVerified;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.usernameChangedAt = usernameChangedAt;
        this.usernamePrevChangedAt = usernamePrevChangedAt;
        this.lang = lang;
        this.privateProfile = privateProfile;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserJpaEntity fromDomain(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getName(),
                user.getUserName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getGender(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getUsernameChangedAt(),
                user.getUsernamePrevChangedAt(),
                user.getLang(),
                user.isPrivateProfile(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public User toDomain() {
        return User.restore(
                this.id,
                this.name,
                this.userName,
                this.email,
                this.emailVerified,
                this.gender,
                this.phoneNumber,
                this.avatarUrl,
                this.usernameChangedAt,
                this.usernamePrevChangedAt,
                this.lang,
                this.privateProfile,
                this.status,
                this.createdAt,
                this.updatedAt
        );
    }
}
