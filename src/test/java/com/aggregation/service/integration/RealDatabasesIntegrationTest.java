package com.aggregation.service.integration;

import com.aggregation.service.adapter.persistence.mongo.MongoUserDocument;
import com.aggregation.service.adapter.persistence.mongo.MongoUserRepository;
import com.aggregation.service.adapter.persistence.postgres.PostgresUserEntity;
import com.aggregation.service.adapter.persistence.postgres.PostgresUserRepository;
import com.aggregation.service.application.UserAggregationService;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RealDatabasesIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("aggregation")
                    .withUsername("aggregation")
                    .withPassword("aggregation");

    @Container
    static final MongoDBContainer MONGODB =
            new MongoDBContainer("mongo:6-jammy");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private PostgresUserRepository postgresUserRepository;

    @Autowired
    private MongoUserRepository mongoUserRepository;

    @Autowired
    private UserAggregationService userAggregationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    }

    @Test
    void flywayAndBothPersistenceAdaptersWorkAgainstRealDatabases() {
        postgresUserRepository.save(PostgresUserEntity.builder()
                .username("same-user")
                .name("Postgres")
                .surname("User")
                .build());
        mongoUserRepository.save(MongoUserDocument.builder()
                .id("mongo-user")
                .username("same-user")
                .firstName("Mongo")
                .lastName("User")
                .build());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                Long.class
        )).isEqualTo(1L);
        assertThat(userAggregationService.searchUsers(null, null, 0, 10).items())
                .extracting(AggregatedUser::source)
                .containsExactly(
                        UserSource.POSTGRESQL,
                        UserSource.MONGODB
                );
    }
}
