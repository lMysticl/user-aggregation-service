package com.aggregation.service.repository.mongo;

import com.aggregation.service.model.MongoUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoUserRepository extends MongoRepository<MongoUser, String> {
    List<MongoUser> findByUsernameContainingIgnoreCase(String username);

    List<MongoUser> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName);
}
