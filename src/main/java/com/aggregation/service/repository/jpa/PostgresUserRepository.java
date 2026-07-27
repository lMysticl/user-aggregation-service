package com.aggregation.service.repository.jpa;

import com.aggregation.service.persistence.jpa.PostgresUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostgresUserRepository extends JpaRepository<PostgresUserEntity, String> {
    List<PostgresUserEntity> findByUsernameContainingIgnoreCase(String username);

    List<PostgresUserEntity> findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
            String name,
            String surname);
}
