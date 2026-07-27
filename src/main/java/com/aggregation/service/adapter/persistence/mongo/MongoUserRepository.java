package com.aggregation.service.adapter.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MongoUserRepository extends MongoRepository<MongoUserDocument, String> {
    List<MongoUserDocument> findByUsernameContainingIgnoreCase(
            String username,
            Pageable pageable);

    List<MongoUserDocument> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName,
            Pageable pageable);
}
