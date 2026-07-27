package com.aggregation.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories("com.aggregation.service.adapter.persistence.postgres")
@EnableMongoRepositories("com.aggregation.service.adapter.persistence.mongo")
public class PersistenceConfiguration {
}
