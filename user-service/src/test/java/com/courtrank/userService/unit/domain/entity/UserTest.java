package com.courtrank.userService.unit.domain.entity;

import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserGender;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {
    @Test
    void create_shouldInitializeVisiblePublicProfileWithEmailVerificationState() {
        UUID id = UUID.randomUUID();

        User user = User.create(id, "Sebastian", "sebas", "sebas@test.com", true);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo("Sebastian");
        assertThat(user.getUserName()).isEqualTo("sebas");
        assertThat(user.getEmail()).isEqualTo("sebas@test.com");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isPrivateProfile()).isFalse();
        assertThat(user.getStatus()).isEqualTo(UserProfileStatus.VISIBLE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void restore_shouldRoundTripAllStoredFields() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-02-01T00:00:00Z");
        Instant usernameChangedAt = Instant.parse("2026-01-15T00:00:00Z");

        User user = User.restore(
                id,
                "Sebastian",
                "sebas",
                "sebas@test.com",
                true,
                UserGender.MALE,
                "+573001112233",
                "https://cdn.test/avatar.png",
                usernameChangedAt,
                null,
                "es",
                true,
                UserProfileStatus.HIDDEN,
                createdAt,
                updatedAt
        );

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getGender()).isEqualTo(UserGender.MALE);
        assertThat(user.getPhoneNumber()).isEqualTo("+573001112233");
        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.test/avatar.png");
        assertThat(user.getUsernameChangedAt()).isEqualTo(usernameChangedAt);
        assertThat(user.getLang()).isEqualTo("es");
        assertThat(user.isPrivateProfile()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserProfileStatus.HIDDEN);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void changeUsername_shouldTrackTwoMostRecentChangesAndThenBlockWithinWindow() {
        Instant now = Instant.parse("2026-06-01T00:00:00Z");
        User user = User.restore(
                UUID.randomUUID(),
                "Sebastian",
                "sebas",
                "sebas@test.com",
                false,
                null,
                null,
                null,
                now.minusSeconds(10),
                now.minusSeconds(20),
                null,
                false,
                UserProfileStatus.VISIBLE,
                now.minusSeconds(30),
                now.minusSeconds(30)
        );

        assertThat(user.getUsernameChangeInfo(now).changesUsed()).isEqualTo(2);
        assertThat(user.getUsernameChangeInfo(now).changesLeft()).isZero();
        assertThatThrownBy(() -> user.assertUsernameCanBeChanged(now))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void mutators_shouldUpdateProfileFieldsAndStatus() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");

        user.changeName("Sebastian Sanchez");
        user.changeGender(UserGender.MALE);
        user.changePhoneNumber("+573001112233");
        user.changeAvatarUrl("https://cdn.test/avatar.png");
        user.changeLang("en");
        user.markEmailVerified();
        user.changePrivacy(true);
        user.hideProfile();
        user.showProfile();
        user.suspendProfile();

        assertThat(user.getName()).isEqualTo("Sebastian Sanchez");
        assertThat(user.getGender()).isEqualTo(UserGender.MALE);
        assertThat(user.getPhoneNumber()).isEqualTo("+573001112233");
        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.test/avatar.png");
        assertThat(user.getLang()).isEqualTo("en");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isPrivateProfile()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserProfileStatus.SUSPENDED);
    }

    @Test
    void markProfileAsDeleted_shouldClearUsernameAndSetDeletedStatus() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");

        user.markProfileAsDeleted();

        assertThat(user.getUserName()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserProfileStatus.DELETED);
    }
}
