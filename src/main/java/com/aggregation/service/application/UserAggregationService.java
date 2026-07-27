package com.aggregation.service.application;

import com.aggregation.service.application.port.UserReadSource;
import com.aggregation.service.application.port.UserWriter;
import com.aggregation.service.config.properties.AggregationProperties;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import com.aggregation.service.exception.SourceUnavailableException;
import com.aggregation.service.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
public class UserAggregationService {
    static final String USER_SEARCH_CACHE = "user-searches";

    private final List<UserReadSource> readSources;
    private final UserReadSource postgresReadSource;
    private final UserWriter userWriter;
    private final Executor aggregationExecutor;
    private final long queryTimeoutMillis;

    public UserAggregationService(
            List<UserReadSource> readSources,
            UserWriter userWriter,
            @Qualifier("aggregationExecutor") Executor aggregationExecutor,
            AggregationProperties properties) {
        this.readSources = readSources.stream()
                .sorted(Comparator.comparing(UserReadSource::source))
                .toList();
        this.postgresReadSource = this.readSources.stream()
                .filter(source -> source.source() == UserSource.POSTGRESQL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "PostgreSQL user read source is required"
                ));
        this.userWriter = userWriter;
        this.aggregationExecutor = aggregationExecutor;
        this.queryTimeoutMillis = properties.getQueryTimeout().toMillis();
    }

    @Cacheable(
            cacheNames = USER_SEARCH_CACHE,
            key = "T(java.util.Arrays).asList(#username, #name)",
            sync = true
    )
    public List<AggregatedUser> searchUsers(String username, String name) {
        String normalizedUsername = normalize(username);
        String normalizedName = normalize(name);
        UserSearchCriteria criteria = new UserSearchCriteria(
                normalizedUsername,
                normalizedUsername == null ? normalizedName : null
        );
        return aggregate(criteria);
    }

    @CacheEvict(cacheNames = USER_SEARCH_CACHE, allEntries = true)
    public AggregatedUser createUser(CreateUserCommand command) {
        return userWriter.create(command);
    }

    public AggregatedUser getUser(String id) {
        return postgresReadSource.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private List<AggregatedUser> aggregate(UserSearchCriteria criteria) {
        List<CompletableFuture<List<AggregatedUser>>> queries = readSources.stream()
                .map(source -> querySource(
                        source.source(),
                        () -> source.search(criteria)
                ))
                .toList();

        try {
            return queries.stream()
                    .flatMap(query -> query.join().stream())
                    .toList();
        } catch (CompletionException exception) {
            queries.forEach(query -> query.cancel(true));
            Throwable cause = unwrap(exception);
            if (cause instanceof SourceUnavailableException sourceUnavailableException) {
                throw sourceUnavailableException;
            }
            throw exception;
        }
    }

    private CompletableFuture<List<AggregatedUser>> querySource(
            UserSource source,
            Supplier<List<AggregatedUser>> query) {
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
                                source.displayName(),
                                cause.getClass().getSimpleName()
                        );
                        throw new CompletionException(
                                new SourceUnavailableException(source.displayName(), cause)
                        );
                    });
        } catch (RejectedExecutionException exception) {
            log.error("{} user query rejected: executor saturated", source.displayName());
            return CompletableFuture.failedFuture(
                    new SourceUnavailableException(source.displayName(), exception)
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
