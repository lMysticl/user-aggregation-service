package com.aggregation.service.application;

import com.aggregation.service.application.port.UserReadSource;
import com.aggregation.service.application.port.UserWriter;
import com.aggregation.service.config.properties.AggregationProperties;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import com.aggregation.service.exception.SourceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAggregationServiceTest {

    @Mock
    private UserReadSource postgresSource;

    @Mock
    private UserReadSource mongoSource;

    @Mock
    private UserWriter userWriter;

    private ExecutorService executor;
    private UserAggregationService userAggregationService;
    private AggregatedUser postgresUser;
    private AggregatedUser mongoUser;

    @BeforeEach
    void setUp() {
        when(postgresSource.source()).thenReturn(UserSource.POSTGRESQL);
        when(mongoSource.source()).thenReturn(UserSource.MONGODB);

        executor = Executors.newFixedThreadPool(2);
        AggregationProperties properties = new AggregationProperties();
        properties.setQueryTimeout(Duration.ofSeconds(1));
        userAggregationService = new UserAggregationService(
                List.of(mongoSource, postgresSource),
                userWriter,
                executor,
                properties
        );

        postgresUser = new AggregatedUser(
                UserSource.POSTGRESQL,
                "7d6d939c-74c2-45a1-924c-8ba608a7b1cf",
                "user-1",
                "User",
                "Userenko"
        );
        mongoUser = new AggregatedUser(
                UserSource.MONGODB,
                "7d6d939c-74c2-45a1-924c-8ba608a7b3",
                "user-2",
                "Testuser",
                "Testov"
        );
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void searchUsersAggregatesPortsInStableSourceOrder() {
        UserSearchCriteria criteria = new UserSearchCriteria(null, null);
        when(postgresSource.search(criteria, 100)).thenReturn(List.of(postgresUser));
        when(mongoSource.search(criteria, 100)).thenReturn(List.of(mongoUser));

        List<AggregatedUser> result = userAggregationService.searchUsers(null, null);

        assertThat(result)
                .extracting(AggregatedUser::username)
                .containsExactly("user-1", "user-2");
        assertThat(result)
                .extracting(AggregatedUser::source)
                .containsExactly(UserSource.POSTGRESQL, UserSource.MONGODB);
    }

    @Test
    void searchUsersUsesUsernameWhenBothFiltersArePresent() {
        UserSearchCriteria criteria = new UserSearchCriteria("user", null);
        when(postgresSource.search(criteria, 100)).thenReturn(List.of(postgresUser));
        when(mongoSource.search(criteria, 100)).thenReturn(List.of(mongoUser));

        List<AggregatedUser> result =
                userAggregationService.searchUsers(" user ", "ignored");

        assertThat(result).hasSize(2);
    }

    @Test
    void searchUsersFindsNameAcrossBothSources() {
        UserSearchCriteria criteria = new UserSearchCriteria(null, "User");
        when(postgresSource.search(criteria, 100)).thenReturn(List.of(postgresUser));
        when(mongoSource.search(criteria, 100)).thenReturn(List.of());

        List<AggregatedUser> result = userAggregationService.searchUsers(null, "User");

        assertThat(result)
                .extracting(AggregatedUser::username)
                .containsExactly("user-1");
    }

    @Test
    void getUserUsesThePostgresReadPort() {
        when(postgresSource.findById(postgresUser.sourceId()))
                .thenReturn(Optional.of(postgresUser));

        assertThat(userAggregationService.getUser(postgresUser.sourceId()))
                .isEqualTo(postgresUser);
    }

    @Test
    void createUserUsesTheConfiguredWriter() {
        CreateUserCommand command = new CreateUserCommand("user-1", "User", "Userenko");
        when(userWriter.create(command)).thenReturn(postgresUser);

        assertThat(userAggregationService.createUser(command))
                .isEqualTo(postgresUser);
    }

    @Test
    void searchUsersFailsWhenOneSourceIsUnavailable() {
        UserSearchCriteria criteria = new UserSearchCriteria(null, null);
        when(postgresSource.search(criteria, 100))
                .thenThrow(new IllegalStateException("PostgreSQL unavailable"));

        assertThatThrownBy(() -> userAggregationService.searchUsers(null, null))
                .isInstanceOf(SourceUnavailableException.class)
                .hasMessageContaining("PostgreSQL");
    }

    @Test
    void searchUsersTimesOutSlowSources() {
        AggregationProperties properties = new AggregationProperties();
        properties.setQueryTimeout(Duration.ofMillis(25));
        UserAggregationService shortTimeoutService = new UserAggregationService(
                List.of(postgresSource, mongoSource),
                userWriter,
                executor,
                properties
        );
        UserSearchCriteria criteria = new UserSearchCriteria(null, null);
        when(postgresSource.search(criteria, 100)).thenAnswer(invocation -> {
            Thread.sleep(250);
            return List.of(postgresUser);
        });
        when(mongoSource.search(criteria, 100)).thenReturn(List.of(mongoUser));

        assertThatThrownBy(() -> shortTimeoutService.searchUsers(null, null))
                .isInstanceOf(SourceUnavailableException.class)
                .hasMessageContaining("PostgreSQL");
    }

    @Test
    void searchUsersFailsWhenAggregationExecutorIsSaturated() {
        AggregationProperties properties = new AggregationProperties();
        UserAggregationService saturatedService = new UserAggregationService(
                List.of(postgresSource, mongoSource),
                userWriter,
                task -> {
                    throw new RejectedExecutionException("queue full");
                },
                properties
        );

        assertThatThrownBy(() -> saturatedService.searchUsers(null, null))
                .isInstanceOf(SourceUnavailableException.class)
                .hasMessageContaining("PostgreSQL");
    }

    @Test
    void paginatedSearchKeepsDuplicateUsernamesAndUsesStableGlobalOrder() {
        UserSearchCriteria criteria = new UserSearchCriteria(null, null);
        AggregatedUser postgresDuplicate = new AggregatedUser(
                UserSource.POSTGRESQL,
                "pg-duplicate",
                "same-user",
                "Postgres",
                "User"
        );
        AggregatedUser mongoDuplicate = new AggregatedUser(
                UserSource.MONGODB,
                "mongo-duplicate",
                "same-user",
                "Mongo",
                "User"
        );
        when(postgresSource.search(criteria, 3))
                .thenReturn(List.of(postgresUser, postgresDuplicate));
        when(mongoSource.search(criteria, 3))
                .thenReturn(List.of(mongoUser, mongoDuplicate));

        UserPage result = userAggregationService.searchUsers(null, null, 0, 2);

        assertThat(result.items())
                .extracting(AggregatedUser::source, AggregatedUser::sourceId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                UserSource.POSTGRESQL,
                                "pg-duplicate"
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                UserSource.MONGODB,
                                "mongo-duplicate"
                        )
                );
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void paginatedSearchRejectsUnboundedPageArguments() {
        assertThatThrownBy(
                () -> userAggregationService.searchUsers(null, null, 101, 20)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
        assertThatThrownBy(
                () -> userAggregationService.searchUsers(null, null, 0, 101)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void getUserUsesTheRequestedReadSource() {
        when(mongoSource.findById(mongoUser.sourceId()))
                .thenReturn(Optional.of(mongoUser));

        assertThat(userAggregationService.getUser(
                UserSource.MONGODB,
                mongoUser.sourceId()
        )).isEqualTo(mongoUser);
    }
}
