package com.aggregation.service.adapter.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoUserRepository extends MongoRepository<MongoUserDocument, String> {
    List<MongoUserDocument> findByUsernameContainingIgnoreCase(String username);

    List<MongoUserDocument> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName);
}
