package com.aggregation.service.config;

import com.aggregation.service.adapter.persistence.mongo.MongoUserDocument;
import com.aggregation.service.adapter.persistence.mongo.MongoUserRepository;
import com.aggregation.service.adapter.persistence.postgres.PostgresUserEntity;
import com.aggregation.service.adapter.persistence.postgres.PostgresUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "aggregation.demo-data.enabled", havingValue = "true")
public class DataInitializer {

    @Bean
    CommandLineRunner initializeDemoData(
            PostgresUserRepository postgresRepo,
            MongoUserRepository mongoRepo) {
        return args -> {
            if (postgresRepo.count() == 0) {
                List<PostgresUserEntity> postgresUsers = List.of(
                        PostgresUserEntity.builder()
                                .username("user-1")
                                .name("User")
                                .surname("Userenko")
                                .build()
                );
                postgresRepo.saveAll(postgresUsers);
                log.info("Added {} demo users to PostgreSQL", postgresUsers.size());
            }

            if (mongoRepo.count() == 0) {
                List<MongoUserDocument> mongoUsers = List.of(
                        MongoUserDocument.builder()
                                .id("7d6d939c-74c2-45a1-924c-8ba608a7b3")
                                .username("user-2")
                                .firstName("Testuser")
                                .lastName("Testov")
                                .build()
                );
                mongoRepo.saveAll(mongoUsers);
                log.info("Added {} demo users to MongoDB", mongoUsers.size());
            }
        };
    }
}
