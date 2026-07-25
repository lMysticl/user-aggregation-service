# User Aggregation Service

[![CI](https://github.com/lMysticl/Aggregation_Service/actions/workflows/ci.yml/badge.svg)](https://github.com/lMysticl/Aggregation_Service/actions/workflows/ci.yml)

A Java 21 and Spring Boot service that exposes one REST API over relational user data and MongoDB user documents. Reads are executed against both repositories and returned through a single `User` response model; new users are written to the relational store.

This repository is a local portfolio/demo service. See [Current limitations](#current-limitations) before connecting it to persistent or valuable data.

## Quick start with Docker

### Prerequisites

- Docker Engine with Docker Compose v2
- Two local-only passwords for PostgreSQL and MongoDB

Clone the repository and create an untracked local environment file:

```bash
git clone https://github.com/lMysticl/Aggregation_Service.git
cd Aggregation_Service
cp .env.example .env
```

PowerShell equivalent:

```powershell
git clone https://github.com/lMysticl/Aggregation_Service.git
Set-Location Aggregation_Service
Copy-Item .env.example .env
```

Set the empty `POSTGRES_PASSWORD` and `MONGO_INITDB_ROOT_PASSWORD` values in `.env`, then build and start the stack:

```bash
docker compose up --build
```

The Compose stack binds only to the local machine:

| Interface | URL |
| --- | --- |
| REST API | http://127.0.0.1:8080/api/users |
| Swagger UI | http://127.0.0.1:8080/swagger-ui/index.html |
| OpenAPI document | http://127.0.0.1:8080/v3/api-docs |
| Health | http://127.0.0.1:8080/actuator/health |

Stop the stack without deleting database volumes:

```bash
docker compose down
```

To intentionally remove all local database volumes as well:

```bash
docker compose down --volumes
```

## Build and test

### Prerequisites

| Dependency | Version | Notes |
| --- | --- | --- |
| JDK | 21 | The project compiles with Java release 21. |
| MongoDB | 6.x | Required at `localhost:27017` by the integration test. |
| Maven | Wrapper-provided 3.9.9 | A separate Maven installation is not required. |

Start an unauthenticated, disposable MongoDB instance for local tests:

```bash
docker run --rm --name aggregation-service-test-mongo -p 127.0.0.1:27017:27017 mongo:6-jammy
```

In another terminal, run the complete clean quality gate:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

PowerShell:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Useful narrower commands:

```bash
# Unit and integration tests
./mvnw test

# Clean package without running tests
./mvnw clean package -DskipTests

# Run only the repository-free unit tests
./mvnw -Dtest=UserAggregationServiceTest test
```

On Windows, replace `./mvnw` with `.\mvnw.cmd`.

GitHub Actions runs `clean verify` on JDK 21 and supplies its own MongoDB 6 service.

## Run from the JVM

The default profile uses two in-memory H2 configurations and expects MongoDB at `mongodb://localhost:27017/users`. With MongoDB running:

```bash
./mvnw spring-boot:run
```

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The application recreates demo users at startup. Do not point this profile at data that must be retained.

## Configuration

### Default JVM profile

All connection settings can be overridden through environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `AGGREGATION_PRIMARY_JDBC_STRATEGY` | `h2` | Primary JDBC driver strategy: `h2` or `postgres` |
| `AGGREGATION_PRIMARY_JDBC_URL` | `jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1` | Primary relational source |
| `AGGREGATION_PRIMARY_JDBC_USERNAME` | `sa` | Primary source username |
| `AGGREGATION_PRIMARY_JDBC_PASSWORD` | empty | Primary source password |
| `AGGREGATION_SECONDARY_JDBC_STRATEGY` | `h2` | Secondary JDBC driver strategy: `h2` or `postgres` |
| `AGGREGATION_SECONDARY_JDBC_URL` | `jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1` | Secondary relational source |
| `AGGREGATION_SECONDARY_JDBC_USERNAME` | `sa` | Secondary source username |
| `AGGREGATION_SECONDARY_JDBC_PASSWORD` | empty | Secondary source password |
| `AGGREGATION_MONGODB_URI` | `mongodb://localhost:27017/users` | MongoDB connection string |

The empty JDBC passwords apply only to the in-memory H2 demo defaults. When
using PostgreSQL, set the matching strategy to `postgres` together with its
JDBC URL and credentials.

### Docker Compose profile

Docker Compose reads `.env`; that file is ignored by Git. The tracked `.env.example` contains names and non-secret defaults, but intentionally leaves passwords empty.

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `POSTGRES_DB` | No | `users_db` | PostgreSQL database |
| `POSTGRES_USER` | No | `aggregation_user` | PostgreSQL application role |
| `POSTGRES_PASSWORD` | Yes | none | PostgreSQL password |
| `MONGO_INITDB_ROOT_USERNAME` | No | `aggregation_admin` | Local MongoDB administrator |
| `MONGO_INITDB_ROOT_PASSWORD` | Yes | none | Local MongoDB password; use a URI-safe value |
| `MONGO_DATABASE` | No | `users_db` | MongoDB database |

Compose converts these values into the `AGGREGATION_*` variables consumed by the `docker` Spring profile. Missing passwords stop configuration before containers are created.

For production-like environments, inject secrets through the platform's secret manager rather than an `.env` file.

## API

| Method | Path | Behavior |
| --- | --- | --- |
| `GET` | `/api/users` | Return users aggregated from PostgreSQL and MongoDB |
| `GET` | `/api/users?username={value}` | Case-insensitive partial username search |
| `GET` | `/api/users?name={value}` | Case-insensitive partial name or surname search |
| `POST` | `/api/users` | Create a user in the relational store |

When both query parameters are present, `username` takes precedence. The service generates a new UUID for every `POST`, regardless of an `id` supplied by the client.

Example:

```bash
curl "http://127.0.0.1:8080/api/users?username=user"

curl -X POST "http://127.0.0.1:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","name":"John","surname":"Doe"}'
```

The generated OpenAPI document is the authoritative interactive reference for request and response schemas.

## Architecture

```text
HTTP /api/users
       |
UserController
       |
UserAggregationService
       |-------------------------|
PostgresUserRepository     MongoUserRepository
       |                         |
      JPA                  MongoDB documents
       |                         |
       +------ User model -------+
```

- Spring Data JPA manages the relational `users` table.
- Spring Data MongoDB reads the MongoDB `users` collection.
- Read operations query both repositories asynchronously and concatenate their results.
- `DataSourceProperties` binds the ordered `data-sources.sources` configuration.
- Actuator exposes health, info, and metrics endpoints.

## Current limitations

- `DataInitializer` deletes and recreates demo users in both stores every time the application starts.
- Hibernate schema management is set to `update`; there is no versioned migration tool.
- The HTTP API has no authentication or authorization.
- Repository failures are currently logged and converted to an empty result for that source.
- There is no production deployment, rollback procedure, backup policy, SLO, or alerting configuration in this repository.

These constraints make the current configuration suitable for local evaluation, not production data.

## Troubleshooting

| Symptom | Check | Resolution |
| --- | --- | --- |
| Compose reports a required variable is missing | `.env` password values | Copy `.env.example` to `.env` and set both passwords. |
| Port 5432 or 27017 is already allocated | Existing local database or test container | Stop the conflicting process or change the host-side Compose port. |
| Integration test cannot connect to MongoDB | `localhost:27017` | Start the disposable MongoDB command from [Build and test](#build-and-test). |
| Maven compilation fails on a newer JDK | `java -version` and `JAVA_HOME` | Run the wrapper with JDK 21. |
| Application starts with unexpected demo records | Startup logs from `DataInitializer` | This is current demo behavior; do not use valuable data. |

## Contributing

Keep REST paths, JSON fields, ordering, and fallback behavior compatible unless a change is explicitly documented. Before opening a pull request, run:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

Do not commit `.env`, credentials, database dumps, or generated `target/` content.

## License

Copyright 2024 Mystic. All rights reserved.

This repository does not grant a general open-source license. See [LICENSE](LICENSE) and obtain explicit permission before copying, modifying, distributing, or otherwise using the code.
