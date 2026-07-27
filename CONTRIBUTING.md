# Contributing

## Development requirements

- JDK 21
- Docker with MongoDB available on `localhost:27017`

Run the full local quality gate before opening a pull request:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
docker build --tag user-aggregation-service:local .
```

On Windows, use `.\mvnw.cmd` for Maven commands.

## Pull requests

- Keep REST paths and successful response fields backward compatible.
- Add or update tests for validation, caching, failure handling, persistence, or ordering changes.
- Keep database changes in a new Flyway migration; never rewrite an applied migration.
- Do not commit credentials, `.env`, database files, logs, or generated build output.
- Document externally visible changes in `CHANGELOG.md`.

Security vulnerabilities must follow [SECURITY.md](SECURITY.md), not the public issue tracker.
