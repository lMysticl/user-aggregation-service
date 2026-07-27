# Changelog

All notable changes to this project are documented in this file.

## Unreleased

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
