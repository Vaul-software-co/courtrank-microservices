package com.courtrank.userService.unit.infrastructure.persistence.jpa.adapter;

import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.infrastructure.persistence.jpa.adapter.JpaUserRepository;
import com.courtrank.userService.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.courtrank.userService.infrastructure.persistence.jpa.repository.SpringUserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
