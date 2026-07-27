package com.aggregation.service.config;

import com.aggregation.service.config.properties.AggregationProperties;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
public class DatabaseTimeoutConfiguration {

    @Bean
    HibernatePropertiesCustomizer jdbcQueryTimeout(AggregationProperties properties) {
        int timeoutMillis = Math.toIntExact(
                properties.getQueryTimeout().toMillis()
        );
        return hibernateProperties -> hibernateProperties.put(
                "jakarta.persistence.query.timeout",
                timeoutMillis
        );
    }

    @Bean
    MongoClientSettingsBuilderCustomizer mongoDriverTimeouts(
            AggregationProperties properties) {
        Duration timeout = properties.getQueryTimeout();
        return builder -> builder
                .applyToClusterSettings(settings -> settings
                        .serverSelectionTimeout(
                                timeout.toMillis(),
                                TimeUnit.MILLISECONDS
                        ))
                .applyToSocketSettings(settings -> settings
                        .connectTimeout(
                                timeout.toMillis(),
                                TimeUnit.MILLISECONDS
                        )
                        .readTimeout(
                                timeout.toMillis(),
                                TimeUnit.MILLISECONDS
                        ));
    }
}
