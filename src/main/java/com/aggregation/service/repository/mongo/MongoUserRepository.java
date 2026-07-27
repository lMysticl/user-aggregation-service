package com.aggregation.service.repository.mongo;

import com.aggregation.service.persistence.mongo.MongoUserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoUserRepository extends MongoRepository<MongoUserDocument, String> {
    List<MongoUserDocument> findByUsernameContainingIgnoreCase(String username);

    List<MongoUserDocument> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName);
}
