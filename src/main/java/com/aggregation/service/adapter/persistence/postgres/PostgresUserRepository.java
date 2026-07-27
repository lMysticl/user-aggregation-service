package com.aggregation.service.adapter.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostgresUserRepository extends JpaRepository<PostgresUserEntity, String> {
    List<PostgresUserEntity> findByUsernameContainingIgnoreCase(String username);

    List<PostgresUserEntity> findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
            String name,
            String surname);
}
