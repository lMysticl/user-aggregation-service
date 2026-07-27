package com.aggregation.service.service;

import com.aggregation.service.config.properties.AggregationProperties;
import com.aggregation.service.exception.SourceUnavailableException;
import com.aggregation.service.exception.UserNotFoundException;
import com.aggregation.service.model.MongoUser;
import com.aggregation.service.model.User;
import com.aggregation.service.repository.jpa.PostgresUserRepository;
import com.aggregation.service.repository.mongo.MongoUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
@Slf4j
public class UserAggregationService {
    static final String USER_SEARCH_CACHE = "user-searches";

    private final PostgresUserRepository postgresUserRepository;
    private final MongoUserRepository mongoUserRepository;
    private final Executor aggregationExecutor;
    private final long queryTimeoutMillis;

    public UserAggregationService(
            PostgresUserRepository postgresUserRepository,
            MongoUserRepository mongoUserRepository,
            @Qualifier("aggregationExecutor") Executor aggregationExecutor,
            AggregationProperties properties) {
        this.postgresUserRepository = postgresUserRepository;
        this.mongoUserRepository = mongoUserRepository;
        this.aggregationExecutor = aggregationExecutor;
        this.queryTimeoutMillis = properties.getQueryTimeout().toMillis();
    }

    @Cacheable(
            cacheNames = USER_SEARCH_CACHE,
            key = "T(java.util.Arrays).asList(#username, #name)",
            sync = true
    )
    public List<User> searchUsers(String username, String name) {
        String normalizedUsername = normalize(username);
        String normalizedName = normalize(name);

        if (normalizedUsername != null) {
            return aggregate(
                    () -> postgresUserRepository.findByUsernameContainingIgnoreCase(normalizedUsername),
                    () -> mongoUserRepository.findByUsernameContainingIgnoreCase(normalizedUsername)
                            .stream()
                            .map(MongoUser::toUser)
                            .toList()
            );
        }

        if (normalizedName != null) {
            return aggregate(
                    () -> postgresUserRepository
                            .findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
                                    normalizedName,
                                    normalizedName
                            ),
                    () -> mongoUserRepository
                            .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                                    normalizedName,
                                    normalizedName
                            )
                            .stream()
                            .map(MongoUser::toUser)
                            .toList()
            );
        }

        return aggregate(
                postgresUserRepository::findAll,
                () -> mongoUserRepository.findAll().stream()
                        .map(MongoUser::toUser)
                        .toList()
        );
    }

    @CacheEvict(cacheNames = USER_SEARCH_CACHE, allEntries = true)
    public User createUser(User user) {
        return postgresUserRepository.save(user);
    }

    public User getUser(String id) {
        return postgresUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private List<User> aggregate(
            Supplier<List<User>> postgresQuery,
            Supplier<List<User>> mongoQuery) {
        CompletableFuture<List<User>> postgresUsers =
                querySource("PostgreSQL", postgresQuery);
        CompletableFuture<List<User>> mongoUsers =
                querySource("MongoDB", mongoQuery);

        try {
            return Stream.concat(
                            postgresUsers.join().stream(),
                            mongoUsers.join().stream()
                    )
                    .toList();
        } catch (CompletionException exception) {
            postgresUsers.cancel(true);
            mongoUsers.cancel(true);
            Throwable cause = unwrap(exception);
            if (cause instanceof SourceUnavailableException sourceUnavailableException) {
                throw sourceUnavailableException;
            }
            throw exception;
        }
    }

    private CompletableFuture<List<User>> querySource(
            String source,
            Supplier<List<User>> query) {
        try {
            return CompletableFuture
                    .supplyAsync(query, aggregationExecutor)
                    .orTimeout(queryTimeoutMillis, TimeUnit.MILLISECONDS)
                    .handle((users, error) -> {
                        if (error == null) {
                            return List.copyOf(users);
                        }

                        Throwable cause = unwrap(error);
                        log.error(
                                "{} user query failed: {}",
                                source,
                                cause.getClass().getSimpleName()
                        );
                        throw new CompletionException(
                                new SourceUnavailableException(source, cause)
                        );
                    });
        } catch (RejectedExecutionException exception) {
            log.error("{} user query rejected: executor saturated", source);
            return CompletableFuture.failedFuture(
                    new SourceUnavailableException(source, exception)
            );
        }
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
