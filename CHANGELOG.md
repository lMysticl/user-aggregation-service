# Changelog

All notable changes to this project are documented in this file.

## Unreleased

### Added

- Paginated `/api/v2/users` contract with explicit source provenance
- Ports and adapters for independent PostgreSQL and MongoDB persistence models
- ArchUnit dependency rules and real PostgreSQL/MongoDB Testcontainers coverage
- Driver-level timeouts and per-source query metrics
- MIT License

### Changed

- Bounded v1 reads without changing the existing JSON response fields
- Standard HTTP failures now preserve their `400`, `404`, `405`, and `415` statuses
- User identities are now unambiguous by source and source-local ID

## 1.0.0 - 2026-07-28

### Added

- Bounded concurrent aggregation with per-source timeouts
- Caffeine query caching and write-triggered invalidation
- Bean Validation request contract and explicit HTTP error responses
- Flyway relational schema migration
- CodeQL, dependency updates, Docker build verification, SBOM, and attested releases

### Changed

- Upgraded to Spring Boot 3.5 on Java 21
- Replaced custom data-source lifecycle code with Spring Boot-managed connections
- Made demo seeding opt-in and non-destructive
- Return `503 Service Unavailable` instead of silently returning partial data
