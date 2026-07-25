@echo off
setlocal

if "%PGUSER%"=="" (
    echo PGUSER must be set to a PostgreSQL role that can create databases.
    exit /b 1
)

if "%PGPASSWORD%"=="" (
    echo PGPASSWORD must be set in the current environment.
    exit /b 1
)

if "%PGHOST%"=="" set "PGHOST=localhost"
if "%PGPORT%"=="" set "PGPORT=5432"
if "%PGDATABASE%"=="" set "PGDATABASE=postgres"

where psql >nul 2>nul
if errorlevel 1 (
    echo psql was not found on PATH.
    exit /b 1
)

psql -v ON_ERROR_STOP=1 -f create_database.sql
