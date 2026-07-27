package com.aggregation.service.application;

import com.aggregation.service.application.port.UserReadSource;
import com.aggregation.service.application.port.UserSourceMetrics;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
@Slf4j
public class UserAggregationService {
    static final String USER_SEARCH_CACHE = "user-searches";
    static final int LEGACY_SOURCE_LIMIT = 100;
    static final int MAX_PAGE = 100;
    static final int MAX_PAGE_SIZE = 100;

    private static final Comparator<AggregatedUser> USER_ORDER =
            Comparator.comparing(
                            AggregatedUser::username,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    )
                    .thenComparing(AggregatedUser::source)
                    .thenComparing(AggregatedUser::sourceId);

    private final List<UserReadSource> readSources;
    private final Map<UserSource, UserReadSource> readSourceByType;
    private final UserWriter userWriter;
    private final UserSourceMetrics sourceMetrics;
    private final Executor aggregationExecutor;
    private final long queryTimeoutMillis;

    public UserAggregationService(
            List<UserReadSource> readSources,
            UserWriter userWriter,
            UserSourceMetrics sourceMetrics,
            @Qualifier("aggregationExecutor") Executor aggregationExecutor,
            AggregationProperties properties) {
        this.readSources = readSources.stream()
                .sorted(Comparator.comparing(UserReadSource::source))
                .toList();
        this.readSourceByType = indexReadSources(this.readSources);
        requireReadSource(UserSource.POSTGRESQL);
        this.userWriter = userWriter;
        this.sourceMetrics = sourceMetrics;
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
        return aggregate(criteria, LEGACY_SOURCE_LIMIT);
    }

    @Cacheable(
            cacheNames = USER_SEARCH_CACHE,
            key = "T(java.util.Arrays).asList('v2', #username, #name, #page, #size)",
            sync = true
    )
    public UserPage searchUsers(
            String username,
            String name,
            int page,
            int size) {
        validatePage(page, size);
        String normalizedUsername = normalize(username);
        String normalizedName = normalize(name);
        UserSearchCriteria criteria = new UserSearchCriteria(
                normalizedUsername,
                normalizedUsername == null ? normalizedName : null
        );

        int offset = Math.multiplyExact(page, size);
        int fetchLimit = Math.addExact(
                Math.multiplyExact(page + 1, size),
                1
        );
        List<AggregatedUser> merged = aggregate(criteria, fetchLimit).stream()
                .sorted(USER_ORDER)
                .toList();
        int fromIndex = Math.min(offset, merged.size());
        int toIndex = Math.min(fromIndex + size, merged.size());

        return new UserPage(
                merged.subList(fromIndex, toIndex),
                page,
                size,
                merged.size() > toIndex
        );
    }

    @CacheEvict(cacheNames = USER_SEARCH_CACHE, allEntries = true)
    public AggregatedUser createUser(CreateUserCommand command) {
        return userWriter.create(command);
    }

    public AggregatedUser getUser(String id) {
        return getUser(UserSource.POSTGRESQL, id);
    }

    public AggregatedUser getUser(UserSource source, String id) {
        return requireReadSource(source)
                .findById(id)
                .orElseThrow(() -> new UserNotFoundException(source, id));
    }

    private List<AggregatedUser> aggregate(UserSearchCriteria criteria, int limit) {
        List<CompletableFuture<List<AggregatedUser>>> queries = readSources.stream()
                .map(source -> querySource(
                        source.source(),
                        () -> source.search(criteria, limit)
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
        UserSourceMetrics.QueryTimer timer = sourceMetrics.start(source);
        try {
            return CompletableFuture
                    .supplyAsync(query, aggregationExecutor)
                    .orTimeout(queryTimeoutMillis, TimeUnit.MILLISECONDS)
                    .handle((users, error) -> {
                        if (error == null) {
                            timer.stop(UserSourceMetrics.Outcome.SUCCESS);
                            return List.copyOf(users);
                        }

                        Throwable cause = unwrap(error);
                        timer.stop(cause instanceof TimeoutException
                                ? UserSourceMetrics.Outcome.TIMEOUT
                                : UserSourceMetrics.Outcome.FAILURE);
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
            timer.stop(UserSourceMetrics.Outcome.REJECTED);
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

    private Map<UserSource, UserReadSource> indexReadSources(
            List<UserReadSource> sources) {
        EnumMap<UserSource, UserReadSource> indexed = new EnumMap<>(UserSource.class);
        for (UserReadSource source : sources) {
            UserReadSource previous = indexed.put(source.source(), source);
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple user read sources configured for " + source.source()
                );
            }
        }
        return Map.copyOf(indexed);
    }

    private UserReadSource requireReadSource(UserSource source) {
        UserReadSource readSource = readSourceByType.get(source);
        if (readSource == null) {
            throw new IllegalStateException(
                    source.displayName() + " user read source is required"
            );
        }
        return readSource;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || page > MAX_PAGE) {
            throw new IllegalArgumentException(
                    "page must be between 0 and " + MAX_PAGE
            );
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }
}
