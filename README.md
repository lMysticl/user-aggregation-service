# User Aggregation Service

[![CI](https://github.com/lMysticl/user-aggregation-service/actions/workflows/ci.yml/badge.svg)](https://github.com/lMysticl/user-aggregation-service/actions/workflows/ci.yml)
[![CodeQL](https://github.com/lMysticl/user-aggregation-service/actions/workflows/codeql.yml/badge.svg)](https://github.com/lMysticl/user-aggregation-service/actions/workflows/codeql.yml)

A Java 21 and Spring Boot 3.5 service that exposes one REST API over PostgreSQL and MongoDB user data. Reads query both stores concurrently; writes go to PostgreSQL.

This is a portfolio service with production-oriented failure, validation, migration, caching, and observability patterns. It is not deployed as a production system.

## What the project demonstrates

- Concurrent PostgreSQL and MongoDB aggregation on a dedicated bounded executor
- Deterministic `503 Service Unavailable` responses when either source fails or times out
- Caffeine query caching with write-triggered invalidation
- Request validation and explicit `201 Created`, `400`, `409`, and `503` API contracts
- Flyway-managed relational schema migrations
- OpenAPI, Actuator health/metrics, Docker Compose, CI, CodeQL, and attested releases

## Quick start with Docker

### Prerequisites

- Docker Engine with Docker Compose v2
- Two local-only passwords for PostgreSQL and MongoDB

```bash
git clone https://github.com/lMysticl/user-aggregation-service.git
cd user-aggregation-service
cp .env.example .env
```

PowerShell:

```powershell
git clone https://github.com/lMysticl/user-aggregation-service.git
Set-Location user-aggregation-service
Copy-Item .env.example .env
```

Set `POSTGRES_PASSWORD` and `MONGO_INITDB_ROOT_PASSWORD` in `.env`, then start the stack:

```bash
docker compose up --build
```

The Compose stack binds its ports to `127.0.0.1`:

| Interface | URL |
| --- | --- |
| REST API | http://127.0.0.1:8080/api/users |
| Swagger UI | http://127.0.0.1:8080/swagger-ui/index.html |
| OpenAPI document | http://127.0.0.1:8080/v3/api-docs |
| Health | http://127.0.0.1:8080/actuator/health |

The `demo` profile adds sample records only when a store is empty. It never clears existing data.

```bash
docker compose down
```

To intentionally remove the local database volumes:

```bash
docker compose down --volumes
```

## Build and test

| Dependency | Version | Notes |
| --- | --- | --- |
| JDK | 21 | The Maven compiler release and CI runtime |
| MongoDB | 6 or newer | Required at `localhost:27017` by the integration tests |
| Maven | Wrapper-provided | A separate Maven installation is not required |

Start a disposable MongoDB for local tests:

```bash
docker run --rm --name aggregation-service-test-mongo -p 127.0.0.1:27017:27017 mongo:6-jammy
```

Run the complete quality gate:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

PowerShell:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

GitHub Actions runs the same command on Java 21 with a MongoDB service and separately verifies that the Docker image builds.

Tagged releases publish the executable JAR, SHA-256 checksum, CycloneDX SBOM,
and GitHub build-provenance attestation.

## Run from the JVM

With MongoDB available locally:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

The default relational database is in-memory H2. Omit the `demo` profile to start without sample records.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `AGGREGATION_PRIMARY_JDBC_URL` | `jdbc:h2:mem:users;DB_CLOSE_DELAY=-1` | Relational JDBC URL |
| `AGGREGATION_PRIMARY_JDBC_USERNAME` | `sa` | Relational username |
| `AGGREGATION_PRIMARY_JDBC_PASSWORD` | empty | Relational password |
| `AGGREGATION_MONGODB_URI` | `mongodb://localhost:27017/users` | MongoDB connection string |
| `AGGREGATION_QUERY_TIMEOUT` | `2s` | Maximum wait for each source query |
| `AGGREGATION_EXECUTOR_CORE_POOL_SIZE` | `4` | Core aggregation worker count |
| `AGGREGATION_EXECUTOR_MAX_POOL_SIZE` | `8` | Maximum aggregation worker count |
| `AGGREGATION_EXECUTOR_QUEUE_CAPACITY` | `100` | Bounded pending-query capacity |

Docker Compose reads database credentials from an ignored `.env` file. Use a platform secret manager outside local development.

## API

| Method | Path | Success | Behavior |
| --- | --- | --- | --- |
| `GET` | `/api/users` | `200` | Return users from PostgreSQL followed by MongoDB |
| `GET` | `/api/users?username={value}` | `200` | Case-insensitive partial username search |
| `GET` | `/api/users?name={value}` | `200` | Case-insensitive partial name or surname search |
| `POST` | `/api/users` | `201` | Validate and create a PostgreSQL user |

When both filters are supplied, `username` takes precedence. Repeated reads are cached for five minutes; a successful `POST` invalidates all search variants.

```bash
curl "http://127.0.0.1:8080/api/users?username=user"

curl -i -X POST "http://127.0.0.1:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","name":"John","surname":"Doe"}'
```

The service does not silently return partial data. If either source fails or exceeds the configured timeout, the request returns `503` with a correlation ID. Logs identify the source and exception type without copying exception messages that may contain credentials.

## Architecture

```text
HTTP /api/users
       |
UserController --- validation and HTTP contract
       |
UserAggregationService --- Caffeine cache
       |
bounded aggregation executor
       |-------------------------|
PostgresUserRepository     MongoUserRepository
       |                         |
JPA + Flyway                 MongoDB
```

## Operations

| Concern | Entry point |
| --- | --- |
| Liveness/readiness | `/actuator/health/liveness`, `/actuator/health/readiness` |
| Metrics | `/actuator/metrics` |
| API contract | `/v3/api-docs` |
| Local image health | Docker health check against `/actuator/health` |

Health details are not exposed to unauthenticated callers. Logs contain source failures and server-side correlation IDs, but API errors do not expose exception messages.

## Current boundaries

- The HTTP API has no authentication or authorization.
- MongoDB schema evolution is application-managed; Flyway covers only the relational schema.
- This repository does not define production deployment, backups, SLOs, alerting, or disaster recovery.
- Cached responses are local to one application instance.

## Security

See [SECURITY.md](SECURITY.md) for supported versions and private vulnerability reporting. Do not commit `.env`, credentials, database dumps, or generated `target/` content.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Pull requests must pass Java tests, the real MongoDB integration tests, the package build, and the Docker image build.

## License

Copyright 2024 Mystic. All rights reserved.

This repository does not grant a general open-source license. See [LICENSE](LICENSE) before copying, modifying, or distributing the code.
