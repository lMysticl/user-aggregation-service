package com.aggregation.service.adapter.persistence.mongo;

import com.aggregation.service.application.UserSearchCriteria;
import com.aggregation.service.application.port.UserReadSource;
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
public class MongoUserAdapter implements UserReadSource {
    private final MongoUserRepository repository;

    @Override
    public UserSource source() {
        return UserSource.MONGODB;
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
                    .map(MongoUserDocument::toDomain)
                    .toList();
        }
        if (criteria.name() != null) {
            return repository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                            criteria.name(),
                            criteria.name(),
                            firstPage
                    )
                    .stream()
                    .map(MongoUserDocument::toDomain)
                    .toList();
        }
        return repository.findAll(firstPage).stream()
                .map(MongoUserDocument::toDomain)
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
                .map(MongoUserDocument::toDomain);
    }
}
