package com.courtrank.userService.unit.infrastructure.persistence.jpa.adapter;

import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.infrastructure.persistence.jpa.adapter.JpaUserRepository;
import com.courtrank.userService.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.courtrank.userService.infrastructure.persistence.jpa.repository.SpringUserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryTest {
    @Mock
    SpringUserJpaRepository springRepository;

    @InjectMocks
    JpaUserRepository repository;

    @Test
    void save_shouldMapDomainToJpaEntity() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com", true);

        this.repository.save(user);

        ArgumentCaptor<UserJpaEntity> entityCaptor = ArgumentCaptor.forClass(UserJpaEntity.class);
        verify(this.springRepository).save(entityCaptor.capture());
        User mapped = entityCaptor.getValue().toDomain();
        assertThat(mapped.getId()).isEqualTo(user.getId());
        assertThat(mapped.getName()).isEqualTo(user.getName());
        assertThat(mapped.isEmailVerified()).isTrue();
    }

    @Test
    void findById_shouldMapJpaEntityToDomain() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com", true);
        when(this.springRepository.findById(user.getId()))
                .thenReturn(Optional.of(UserJpaEntity.fromDomain(user)));

        Optional<User> result = this.repository.findById(user.getId());

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(user.getId());
        assertThat(result.orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void findByIds_shouldReturnEmptyListWithoutQueryWhenIdsAreEmpty() {
        assertThat(this.repository.findByIds(List.of())).isEmpty();

        verify(this.springRepository, never()).findAllById(any());
    }

    @Test
    void searchPublic_shouldReturnEmptyListWhenQueryIsTooShort() {
        assertThat(this.repository.searchPublic("s", 10, List.of())).isEmpty();

        verify(this.springRepository, never()).searchPublic(any(), any(), any());
        verify(this.springRepository, never()).searchPublicWithExclusions(any(), any(), any(), any());
    }

    @Test
    void searchPublic_shouldTrimQueryAndBoundLimit() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        when(this.springRepository.searchPublic(eq("sebas"), eq(UserProfileStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(List.of(UserJpaEntity.fromDomain(user)));

        List<User> result = this.repository.searchPublic("  sebas  ", 100, List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(this.springRepository).searchPublic(eq("sebas"), eq(UserProfileStatus.VISIBLE), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(user.getId());
    }

    @Test
    void searchPublic_shouldUseExclusionsWhenProvided() {
        UUID excludedId = UUID.randomUUID();
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        when(this.springRepository.searchPublicWithExclusions(
                eq("sebas"),
                eq(UserProfileStatus.VISIBLE),
                eq(List.of(excludedId)),
                any(Pageable.class)
        )).thenReturn(List.of(UserJpaEntity.fromDomain(user)));

        List<User> result = this.repository.searchPublic("sebas", 10, List.of(excludedId));

        verify(this.springRepository).searchPublicWithExclusions(
                eq("sebas"),
                eq(UserProfileStatus.VISIBLE),
                eq(List.of(excludedId)),
                any(Pageable.class)
        );
        assertThat(result).hasSize(1);
    }
}
