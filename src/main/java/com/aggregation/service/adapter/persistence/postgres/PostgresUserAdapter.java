package com.aggregation.service.adapter.persistence.postgres;

import com.aggregation.service.application.CreateUserCommand;
import com.aggregation.service.application.UserSearchCriteria;
import com.aggregation.service.application.port.UserReadSource;
import com.aggregation.service.application.port.UserWriter;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostgresUserAdapter implements UserReadSource, UserWriter {
    private final PostgresUserRepository repository;

    @Override
    public UserSource source() {
        return UserSource.POSTGRESQL;
    }

    @Override
    public List<AggregatedUser> search(UserSearchCriteria criteria) {
        if (criteria.username() != null) {
            return repository.findByUsernameContainingIgnoreCase(criteria.username())
                    .stream()
                    .map(PostgresUserEntity::toDomain)
                    .toList();
        }
        if (criteria.name() != null) {
            return repository
                    .findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
                            criteria.name(),
                            criteria.name()
                    )
                    .stream()
                    .map(PostgresUserEntity::toDomain)
                    .toList();
        }
        return repository.findAll().stream()
                .map(PostgresUserEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<AggregatedUser> findById(String sourceId) {
        return repository.findById(sourceId)
                .map(PostgresUserEntity::toDomain);
    }

    @Override
    public AggregatedUser create(CreateUserCommand command) {
        PostgresUserEntity entity = PostgresUserEntity.builder()
                .username(command.username())
                .name(command.firstName())
                .surname(command.lastName())
                .build();
        return repository.save(entity).toDomain();
    }
}
