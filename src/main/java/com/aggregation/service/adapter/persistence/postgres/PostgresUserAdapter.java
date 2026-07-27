package com.aggregation.service.adapter.persistence.postgres;

import com.aggregation.service.application.CreateUserCommand;
import com.aggregation.service.application.UserSearchCriteria;
import com.aggregation.service.application.port.UserReadSource;
import com.aggregation.service.application.port.UserWriter;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public List<AggregatedUser> search(UserSearchCriteria criteria, int limit) {
        Pageable firstPage = firstPage(limit);
        if (criteria.username() != null) {
            return repository.findByUsernameContainingIgnoreCase(
                            criteria.username(),
                            firstPage
                    )
                    .stream()
                    .map(PostgresUserEntity::toDomain)
                    .toList();
        }
        if (criteria.name() != null) {
            return repository
                    .findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
                            criteria.name(),
                            criteria.name(),
                            firstPage
                    )
                    .stream()
                    .map(PostgresUserEntity::toDomain)
                    .toList();
        }
        return repository.findAll(firstPage).stream()
                .map(PostgresUserEntity::toDomain)
                .toList();
    }

    private Pageable firstPage(int limit) {
        Sort sort = Sort.by(
                Sort.Order.asc("username"),
                Sort.Order.asc("id")
        );
        return PageRequest.of(0, limit, sort);
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
