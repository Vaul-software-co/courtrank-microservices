package com.courtrank.socialService.unit.domain.entity;

import com.courtrank.socialService.domain.entity.SocialUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SocialUserTest {
    @Test
    void create_shouldInitializeSyncedVisibleUserProjection() {
        UUID userId = UUID.randomUUID();
        Instant sourceUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");

        SocialUser user = SocialUser.create(
                userId,
                "Sebastian",
                "sebas",
                "https://cdn.test/avatar.png",
                false,
                true,
                sourceUpdatedAt
        );

        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getName()).isEqualTo("Sebastian");
        assertThat(user.getUsername()).isEqualTo("sebas");
        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.test/avatar.png");
        assertThat(user.isPrivate()).isFalse();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getSourceUpdatedAt()).isEqualTo(sourceUpdatedAt);
        assertThat(user.getSyncedAt()).isNotNull();
        assertThat(user.canBeShown()).isTrue();
    }

    @Test
    void restore_shouldRoundTripAllStoredFields() {
        UUID userId = UUID.randomUUID();
        Instant deletedAt = Instant.parse("2026-01-10T00:00:00Z");
        Instant sourceUpdatedAt = Instant.parse("2026-01-11T00:00:00Z");
        Instant syncedAt = Instant.parse("2026-01-11T00:00:01Z");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-12T00:00:00Z");

        SocialUser user = SocialUser.restore(
                userId,
                "Sebastian",
                "sebas",
                null,
                true,
                false,
                deletedAt,
                sourceUpdatedAt,
                syncedAt,
                createdAt,
                updatedAt
        );

        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.isPrivate()).isTrue();
        assertThat(user.isActive()).isFalse();
        assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(user.getSourceUpdatedAt()).isEqualTo(sourceUpdatedAt);
        assertThat(user.getSyncedAt()).isEqualTo(syncedAt);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void syncProfile_shouldUpdateFromFreshSourceEvent() {
        SocialUser user = SocialUser.create(
                UUID.randomUUID(),
                "Sebastian",
                "sebas",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        user.syncProfile(
                "Sebastian Sanchez",
                "sebastian",
                "https://cdn.test/new-avatar.png",
                true,
                true,
                Instant.parse("2026-01-02T00:00:00Z")
        );

        assertThat(user.getName()).isEqualTo("Sebastian Sanchez");
        assertThat(user.getUsername()).isEqualTo("sebastian");
        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.test/new-avatar.png");
        assertThat(user.isPrivate()).isTrue();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getSourceUpdatedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void syncProfile_shouldIgnoreOlderSourceEvent() {
        SocialUser user = SocialUser.create(
                UUID.randomUUID(),
                "Sebastian",
                "sebas",
                null,
                false,
                true,
                Instant.parse("2026-01-02T00:00:00Z")
        );

        user.syncProfile(
                "Old Name",
                "old",
                "https://cdn.test/old.png",
                true,
                false,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThat(user.getName()).isEqualTo("Sebastian");
        assertThat(user.getUsername()).isEqualTo("sebas");
        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.isPrivate()).isFalse();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getSourceUpdatedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void markDeletedAndRestoreFromSource_shouldToggleVisibility() {
        SocialUser user = SocialUser.create(
                UUID.randomUUID(),
                "Sebastian",
                "sebas",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        user.markDeleted(
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        );

        assertThat(user.isActive()).isFalse();
        assertThat(user.canBeShown()).isFalse();

        user.restoreFromSource(
                "Sebastian",
                "sebas",
                null,
                false,
                true,
                Instant.parse("2026-01-03T00:00:00Z")
        );

        assertThat(user.isActive()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.canBeShown()).isTrue();
    }

    @Test
    void isPubliclyVisibleTo_shouldRespectPrivacyFollowerAndBlockRules() {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        SocialUser user = SocialUser.create(
                ownerId,
                "Sebastian",
                "sebas",
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThat(user.isPubliclyVisibleTo(ownerId, false, false)).isTrue();
        assertThat(user.isPubliclyVisibleTo(viewerId, false, false)).isFalse();
        assertThat(user.isPubliclyVisibleTo(viewerId, true, false)).isTrue();
        assertThat(user.isPubliclyVisibleTo(viewerId, true, true)).isFalse();
    }
}
