package com.aggregation.service.config;

import com.aggregation.service.config.properties.AggregationProperties;
import com.mongodb.MongoClientSettings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTimeoutConfigurationTest {

    @Test
    void appliesTheAggregationTimeoutToJdbcAndMongoDrivers() {
        AggregationProperties properties = new AggregationProperties();
        properties.setQueryTimeout(Duration.ofMillis(750));
        DatabaseTimeoutConfiguration configuration =
                new DatabaseTimeoutConfiguration();

        Map<String, Object> hibernateProperties = new HashMap<>();
        HibernatePropertiesCustomizer jdbcCustomizer =
                configuration.jdbcQueryTimeout(properties);
        jdbcCustomizer.customize(hibernateProperties);

        MongoClientSettings.Builder mongoBuilder = MongoClientSettings.builder();
        MongoClientSettingsBuilderCustomizer mongoCustomizer =
                configuration.mongoDriverTimeouts(properties);
        mongoCustomizer.customize(mongoBuilder);
        MongoClientSettings mongoSettings = mongoBuilder.build();

        assertThat(hibernateProperties)
                .containsEntry("jakarta.persistence.query.timeout", 750);
        assertThat(mongoSettings.getClusterSettings()
                .getServerSelectionTimeout(TimeUnit.MILLISECONDS))
                .isEqualTo(750);
        assertThat(mongoSettings.getSocketSettings()
                .getConnectTimeout(TimeUnit.MILLISECONDS))
                .isEqualTo(750);
        assertThat(mongoSettings.getSocketSettings()
                .getReadTimeout(TimeUnit.MILLISECONDS))
                .isEqualTo(750);
    }
}
