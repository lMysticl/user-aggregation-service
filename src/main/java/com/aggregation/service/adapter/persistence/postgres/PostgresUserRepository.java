package com.aggregation.service.adapter.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostgresUserRepository extends JpaRepository<PostgresUserEntity, String> {
    List<PostgresUserEntity> findByUsernameContainingIgnoreCase(
            String username,
            Pageable pageable);

    List<PostgresUserEntity> findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
            String name,
            String surname,
            Pageable pageable);
}
