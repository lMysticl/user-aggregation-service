package com.aggregation.service.service;

import com.aggregation.service.config.properties.AggregationProperties;
import com.aggregation.service.exception.SourceUnavailableException;
import com.aggregation.service.model.MongoUser;
import com.aggregation.service.model.User;
import com.aggregation.service.repository.jpa.PostgresUserRepository;
import com.aggregation.service.repository.mongo.MongoUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAggregationServiceTest {

    @Mock
    private PostgresUserRepository postgresUserRepository;

    @Mock
    private MongoUserRepository mongoUserRepository;

    private ExecutorService executor;
    private UserAggregationService userAggregationService;
    private User postgresUser;
    private MongoUser mongoUser;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        AggregationProperties properties = new AggregationProperties();
        properties.setQueryTimeout(Duration.ofSeconds(1));
        userAggregationService = new UserAggregationService(
                postgresUserRepository,
                mongoUserRepository,
                executor,
                properties
        );

        postgresUser = User.builder()
                .id("7d6d939c-74c2-45a1-924c-8ba608a7b1cf")
                .username("user-1")
                .name("User")
                .surname("Userenko")
                .build();

        mongoUser = MongoUser.builder()
                .id("7d6d939c-74c2-45a1-924c-8ba608a7b3")
                .username("user-2")
                .firstName("Testuser")
                .lastName("Testov")
                .build();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void searchUsersAggregatesUsersInStableSourceOrder() {
        when(postgresUserRepository.findAll()).thenReturn(List.of(postgresUser));
        when(mongoUserRepository.findAll()).thenReturn(List.of(mongoUser));

        List<User> result = userAggregationService.searchUsers(null, null);

        assertThat(result)
                .extracting(User::getUsername)
                .containsExactly("user-1", "user-2");
    }

    @Test
    void searchUsersUsesUsernameWhenBothFiltersArePresent() {
        when(postgresUserRepository.findByUsernameContainingIgnoreCase("user"))
                .thenReturn(List.of(postgresUser));
        when(mongoUserRepository.findByUsernameContainingIgnoreCase("user"))
                .thenReturn(List.of(mongoUser));

        List<User> result = userAggregationService.searchUsers(" user ", "ignored");

        assertThat(result).hasSize(2);
    }

    @Test
    void searchUsersFindsNameAcrossBothSources() {
        when(postgresUserRepository
                .findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase("User", "User"))
                .thenReturn(List.of(postgresUser));
        when(mongoUserRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("User", "User"))
                .thenReturn(List.of());

        List<User> result = userAggregationService.searchUsers(null, "User");

        assertThat(result)
                .extracting(User::getUsername)
                .containsExactly("user-1");
    }

    @Test
    void searchUsersFailsWhenOneSourceIsUnavailable() {
        when(postgresUserRepository.findAll())
                .thenThrow(new IllegalStateException("PostgreSQL unavailable"));
        when(mongoUserRepository.findAll()).thenReturn(List.of(mongoUser));

        assertThatThrownBy(() -> userAggregationService.searchUsers(null, null))
                .isInstanceOf(SourceUnavailableException.class)
                .hasMessageContaining("PostgreSQL");
    }

    @Test
    void searchUsersTimesOutSlowSources() {
        AggregationProperties properties = new AggregationProperties();
        properties.setQueryTimeout(Duration.ofMillis(25));
        UserAggregationService shortTimeoutService = new UserAggregationService(
                postgresUserRepository,
                mongoUserRepository,
                executor,
                properties
        );
        when(postgresUserRepository.findAll()).thenAnswer(invocation -> {
            Thread.sleep(250);
            return List.of(postgresUser);
        });
        when(mongoUserRepository.findAll()).thenReturn(List.of(mongoUser));

        assertThatThrownBy(() -> shortTimeoutService.searchUsers(null, null))
                .isInstanceOf(SourceUnavailableException.class)
                .hasMessageContaining("PostgreSQL");
    }

    @Test
    void searchUsersFailsWhenAggregationExecutorIsSaturated() {
        AggregationProperties properties = new AggregationProperties();
        UserAggregationService saturatedService = new UserAggregationService(
                postgresUserRepository,
                mongoUserRepository,
                task -> {
                    throw new RejectedExecutionException("queue full");
                },
                properties
        );

        assertThatThrownBy(() -> saturatedService.searchUsers(null, null))
                .isInstanceOf(SourceUnavailableException.class)
                .hasMessageContaining("PostgreSQL");
    }
}
