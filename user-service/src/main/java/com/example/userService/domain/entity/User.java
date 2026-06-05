package com.example.userService.domain.entity;

import com.example.userService.domain.enums.UserGender;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.exceptions.DomainValidationException;
import com.example.userService.domain.valueObjects.UsernameChangeInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class User {
    private static final int USERNAME_CHANGE_LIMIT = 2;
    private static final Duration USERNAME_CHANGE_WINDOW = Duration.ofDays(180);

    private UUID id;
    private String name;
    private String userName;
    private String email;
    private UserGender gender;
    private String phoneNumber;
    private String avatarUrl;
    private Instant usernameChangedAt;
    private Instant usernamePrevChangedAt;
    private String lang;
    private boolean privateProfile;
    private UserProfileStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private User(
            UUID id,
            String name,
            String userName,
            String email,
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

    public static User create(
            UUID id,
            String name,
            String userName,
            String email
    ){
        Instant now = Instant.now();
        return new User(
                id,
                name,
                userName,
                email,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                UserProfileStatus.VISIBLE,
                now,
                now
        );
    }

    public static User restore(
            UUID id,
            String name,
            String userName,
            String email,
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
    ){
        return new User(
                id,
                name,
                userName,
                email,
                gender,
                phoneNumber,
                avatarUrl,
                usernameChangedAt,
                usernamePrevChangedAt,
                lang,
                privateProfile,
                status,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getEmail() {
        return this.email;
    }

    public UserGender getGender() {
        return this.gender;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    public Instant getUsernameChangedAt() {
        return this.usernameChangedAt;
    }

    public Instant getUsernamePrevChangedAt() {
        return this.usernamePrevChangedAt;
    }

    public String getLang() {
        return this.lang;
    }

    public boolean isPrivateProfile() {
        return this.privateProfile;
    }

    public UserProfileStatus getStatus() {
        return this.status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public UsernameChangeInfo getUsernameChangeInfo(Instant now) {
        Instant cutoff = now.minus(USERNAME_CHANGE_WINDOW);
        List<Instant> changesInWindow = Stream.of(
                        this.usernameChangedAt,
                        this.usernamePrevChangedAt
                )
                .filter(Objects::nonNull)
                .filter(date -> date.isAfter(cutoff))
                .toList();

        int changesUsed = changesInWindow.size();
        int changesLeft = Math.max(0, USERNAME_CHANGE_LIMIT - changesUsed);

        Instant nextAvailableAt = null;

        if (changesLeft == 0) {
            Instant oldest = changesInWindow.stream()
                    .min(Instant::compareTo)
                    .orElseThrow();

            nextAvailableAt = oldest.plus(USERNAME_CHANGE_WINDOW);
        }

        return new UsernameChangeInfo(
                changesUsed,
                changesLeft,
                nextAvailableAt
        );
    }

    public void changeUsername(String newUsername) {
        Instant now = Instant.now();
        this.assertUsernameCanBeChanged(now);

        this.usernamePrevChangedAt = this.usernameChangedAt;
        this.usernameChangedAt = now;
        this.userName = newUsername;
        this.updatedAt = now;
    }

    public void assertUsernameCanBeChanged(Instant now) {
        UsernameChangeInfo info = this.getUsernameChangeInfo(now);

        if (info.changesLeft() > 0) {
            return;
        }

        throw new DomainValidationException(
                "Username can only be changed " + USERNAME_CHANGE_LIMIT + " times every 6 months."
        );
    }

    public void changeName(String name){
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void changeGender(UserGender gender){
        this.gender = gender;
        this.updatedAt = Instant.now();
    }

    public void changePhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
        this.updatedAt = Instant.now();
    }

    public void changeAvatarUrl(String avatarUrl){
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }

    public void changeLang(String lang) {
        this.lang = lang;
        this.updatedAt = Instant.now();
    }

    public void changePrivacy(boolean privateProfile) {
        this.privateProfile = privateProfile;
        this.updatedAt = Instant.now();
    }

    public void showProfile() {
        this.status = UserProfileStatus.VISIBLE;
        this.updatedAt = Instant.now();
    }

    public void hideProfile() {
        this.status = UserProfileStatus.HIDDEN;
        this.updatedAt = Instant.now();
    }

    public void suspendProfile() {
        this.status = UserProfileStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void markProfileAsDeleted() {
        this.userName = null;
        this.status = UserProfileStatus.DELETED;
        this.updatedAt = Instant.now();
    }
}
