package com.aggregation.service.integration;

import com.aggregation.service.adapter.persistence.mongo.MongoUserRepository;
import com.aggregation.service.adapter.persistence.postgres.PostgresUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demodb;DB_CLOSE_DELAY=-1",
        "spring.data.mongodb.uri=mongodb://localhost:27017/aggregation_service_demo_test"
})
@ActiveProfiles({"test", "demo"})
class DemoDataIntegrationTest {

    @Autowired
    private PostgresUserRepository postgresUserRepository;

    @Autowired
    private MongoUserRepository mongoUserRepository;

    @AfterEach
    void tearDown() {
        postgresUserRepository.deleteAll();
        mongoUserRepository.deleteAll();
    }

    @Test
    void demoProfileSeedsEmptyStoresWithGeneratedRelationalId() {
        assertThat(postgresUserRepository.findByUsernameContainingIgnoreCase(
                "user-1",
                PageRequest.of(0, 1)
        ))
                .singleElement()
                .satisfies(user -> assertThat(user.getId()).isNotBlank());
        assertThat(mongoUserRepository.findByUsernameContainingIgnoreCase(
                "user-2",
                PageRequest.of(0, 1)
        ))
                .hasSize(1);
    }
}
