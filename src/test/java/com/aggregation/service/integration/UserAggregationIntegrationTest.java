package com.aggregation.service.integration;

import com.aggregation.service.model.MongoUser;
import com.aggregation.service.model.User;
import com.aggregation.service.repository.jpa.PostgresUserRepository;
import com.aggregation.service.repository.mongo.MongoUserRepository;
import com.aggregation.service.service.UserAggregationService;
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

    @BeforeEach
    void setUp() {
        postgresUserRepository.deleteAll();
        mongoUserRepository.deleteAll();
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(Cache::clear);

        postgresUserRepository.save(User.builder()
                .username("testUser")
                .name("Test")
                .surname("User")
                .build());

        mongoUserRepository.save(MongoUser.builder()
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
        List<User> users = userAggregationService.searchUsers(null, null);

        assertThat(users)
                .extracting(User::getUsername)
                .containsExactly("testUser", "mongoUser");
    }

    @Test
    void cachesReadsAndEvictsAfterCreate() {
        assertThat(userAggregationService.searchUsers(null, null)).hasSize(2);

        mongoUserRepository.save(MongoUser.builder()
                .id("cached-mongo-user")
                .username("cachedMongoUser")
                .firstName("Cached")
                .lastName("Mongo")
                .build());

        assertThat(userAggregationService.searchUsers(null, null))
                .as("the second identical read is served from Caffeine")
                .hasSize(2);

        userAggregationService.createUser(User.builder()
                .username("createdUser")
                .name("Created")
                .surname("User")
                .build());

        assertThat(userAggregationService.searchUsers(null, null))
                .as("a write evicts all cached search variants")
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder(
                        "testUser",
                        "createdUser",
                        "mongoUser",
                        "cachedMongoUser"
                );
    }

    @Test
    void treatsSearchTextAsLiteralTextInsteadOfMongoRegex() {
        mongoUserRepository.save(MongoUser.builder()
                .id("literal-dot")
                .username("literal.dot")
                .firstName("Literal")
                .lastName("Dot")
                .build());
        mongoUserRepository.save(MongoUser.builder()
                .id("regex-lookalike")
                .username("literalXdot")
                .firstName("Regex")
                .lastName("Lookalike")
                .build());

        assertThat(userAggregationService.searchUsers(".", null))
                .extracting(User::getUsername)
                .containsExactly("literal.dot");
    }
}
