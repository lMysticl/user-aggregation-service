package com.aggregation.service.adapter.persistence.mongo;

import com.aggregation.service.application.UserSearchCriteria;
import com.aggregation.service.application.port.UserReadSource;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoUserAdapter implements UserReadSource {
    private final MongoUserRepository repository;

    @Override
    public UserSource source() {
        return UserSource.MONGODB;
    }

    @Override
    public List<AggregatedUser> search(UserSearchCriteria criteria) {
        if (criteria.username() != null) {
            return repository.findByUsernameContainingIgnoreCase(criteria.username())
                    .stream()
                    .map(MongoUserDocument::toDomain)
                    .toList();
        }
        if (criteria.name() != null) {
            return repository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                            criteria.name(),
                            criteria.name()
                    )
                    .stream()
                    .map(MongoUserDocument::toDomain)
                    .toList();
        }
        return repository.findAll().stream()
                .map(MongoUserDocument::toDomain)
                .toList();
    }

    @Override
    public Optional<AggregatedUser> findById(String sourceId) {
        return repository.findById(sourceId)
                .map(MongoUserDocument::toDomain);
    }
}
