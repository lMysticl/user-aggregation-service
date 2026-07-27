# Evolve the Service to Ports and Adapters

Status: Accepted

## Context

The service aggregates user records from PostgreSQL and MongoDB in one Spring Boot
deployment. The original `User` type is simultaneously a JPA entity, an HTTP
response, and the aggregation model. MongoDB records are converted into that JPA
entity, so the API cannot identify the owning source. The unversioned HTTP API is
already public and must remain usable while the internal boundaries evolve.

The following quality scenarios drive the change:

- Changing a persistence mapping must not change an HTTP response type.
- Records with the same username or identifier in different stores must remain
  distinguishable.
- A request must read a bounded number of records in a deterministic order.
- A slow or unavailable source must fail within configured driver and application
  timeouts and expose a source-specific metric.
- PostgreSQL migrations and queries must be exercised against PostgreSQL in CI.

## Options

1. Rewrite or split the application into services. This adds deployment, network,
   consistency, and operational cost without a demonstrated independent-scaling or
   team-ownership requirement.
2. Keep the current layered model and add mapping helpers. This leaves the core
   coupled to Spring Data types and does not create an enforceable persistence
   boundary.
3. Incrementally introduce an immutable canonical model, application use cases and
   persistence ports inside the existing monolith.

## Decision

Choose option 3.

- Keep one Spring Boot application and one deployment unit.
- Keep `/api/users` compatible during migration.
- Introduce `/api/v2/users` for the paginated, provenance-aware contract.
- Treat PostgreSQL and MongoDB records as distinct. Matching usernames are not
  deduplicated because the service has no authoritative cross-source identity rule.
- Order aggregated results by username, source, then source identifier.
- Allow dependencies from HTTP and persistence adapters toward application/domain
  code, never from application/domain code toward Spring Web, JPA, or MongoDB.
- PostgreSQL remains the sole writer used by the create-user use case.

## Consequences

The service gains explicit source identity, independently evolvable transport and
persistence models, bounded reads, and testable dependency direction. It also gains
mapping code and a versioned API that must be maintained while v1 remains supported.
No distributed transaction, event delivery, CQRS, or independent deployment is
introduced.

## Confirmation

- MockMvc contract tests protect v1 status codes and the create-user `Location`.
- ArchUnit rules enforce adapter-to-core dependency direction.
- Testcontainers exercises Flyway, PostgreSQL, and MongoDB together in CI.
- Driver timeout configuration and per-source timers are checked in tests and
  exposed through Actuator metrics.

Reassess the single-deployment decision only when independent scaling, failure
isolation, regulatory separation, or team ownership becomes a measured constraint.
