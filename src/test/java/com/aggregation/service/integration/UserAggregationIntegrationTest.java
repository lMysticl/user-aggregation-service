package com.aggregation.service.integration;

import com.aggregation.service.adapter.persistence.mongo.MongoUserDocument;
import com.aggregation.service.adapter.persistence.mongo.MongoUserRepository;
import com.aggregation.service.adapter.metrics.MicrometerUserSourceMetrics;
import com.aggregation.service.adapter.persistence.postgres.PostgresUserEntity;
import com.aggregation.service.adapter.persistence.postgres.PostgresUserRepository;
import com.aggregation.service.application.CreateUserCommand;
import com.aggregation.service.application.UserAggregationService;
import com.aggregation.service.domain.AggregatedUser;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserAggregationIntegrationTest {

    @Autowired
    private UserAggregationService userAggregationService;

    @Autowired
    private PostgresUserRepository postgresUserRepository;

    @Autowired
    private MongoUserRepository mongoUserRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        postgresUserRepository.deleteAll();
        mongoUserRepository.deleteAll();
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(Cache::clear);

        postgresUserRepository.save(PostgresUserEntity.builder()
                .username("testUser")
                .name("Test")
                .surname("User")
                .build());

        mongoUserRepository.save(MongoUserDocument.builder()
                .id("mongo-user")
                .username("mongoUser")
                .firstName("Mongo")
                .lastName("User")
                .build());
    }

    @AfterEach
    void tearDown() {
        postgresUserRepository.deleteAll();
        mongoUserRepository.deleteAll();
    }

    @Test
    void aggregatesUsersFromPostgresAndMongo() {
        List<AggregatedUser> users = userAggregationService.searchUsers(null, null);

        assertThat(users)
                .extracting(AggregatedUser::username)
                .containsExactly("testUser", "mongoUser");
    }

    @Test
    void cachesReadsAndEvictsAfterCreate() {
        assertThat(userAggregationService.searchUsers(null, null)).hasSize(2);

        mongoUserRepository.save(MongoUserDocument.builder()
                .id("cached-mongo-user")
                .username("cachedMongoUser")
                .firstName("Cached")
                .lastName("Mongo")
                .build());

        assertThat(userAggregationService.searchUsers(null, null))
                .as("the second identical read is served from Caffeine")
                .hasSize(2);

        userAggregationService.createUser(new CreateUserCommand(
                "createdUser",
                "Created",
                "User"
        ));

        assertThat(userAggregationService.searchUsers(null, null))
                .as("a write evicts all cached search variants")
                .extracting(AggregatedUser::username)
                .containsExactlyInAnyOrder(
                        "testUser",
                        "createdUser",
                        "mongoUser",
                        "cachedMongoUser"
                );
    }

    @Test
    void treatsSearchTextAsLiteralTextInsteadOfMongoRegex() {
        mongoUserRepository.save(MongoUserDocument.builder()
                .id("literal-dot")
                .username("literal.dot")
                .firstName("Literal")
                .lastName("Dot")
                .build());
        mongoUserRepository.save(MongoUserDocument.builder()
                .id("regex-lookalike")
                .username("literalXdot")
                .firstName("Regex")
                .lastName("Lookalike")
                .build());

        assertThat(userAggregationService.searchUsers(".", null))
                .extracting(AggregatedUser::username)
                .containsExactly("literal.dot");
    }

    @Test
    void exposesOneSuccessfulTimerForEachConfiguredSource() {
        userAggregationService.searchUsers(null, null);

        assertThat(meterRegistry.find(MicrometerUserSourceMetrics.QUERY_METRIC)
                .tags("source", "postgresql", "outcome", "success")
                .timer())
                .isNotNull()
                .satisfies(timer -> assertThat(timer.count()).isPositive());
        assertThat(meterRegistry.find(MicrometerUserSourceMetrics.QUERY_METRIC)
                .tags("source", "mongodb", "outcome", "success")
                .timer())
                .isNotNull()
                .satisfies(timer -> assertThat(timer.count()).isPositive());
    }
}
