package com.aggregation.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories("com.aggregation.service.repository.jpa")
@EnableMongoRepositories("com.aggregation.service.repository.mongo")
public class PersistenceConfiguration {
}
