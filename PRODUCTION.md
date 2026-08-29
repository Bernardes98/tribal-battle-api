# Tribal Battle API — Production

## Required environment

Copy `.env.example` to the environment-variable manager of the hosting provider. Do not commit a real `.env` file.

Production must use `SPRING_PROFILES_ACTIVE=prod`, a managed PostgreSQL database, HTTPS at the edge/proxy, and exact frontend origins in `FRONTEND_ORIGINS`.

## Health checks

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

Only `health` and `info` are exposed through Actuator.

## Password reset

Configure SMTP variables and `FRONTEND_BASE_URL`. Reset tokens are never stored raw; only SHA-256 hashes are persisted. Successful reset revokes all active account sessions.

## Backups

Enable automated PostgreSQL backups at the provider. Before launch, restore one backup into a temporary database and start the API against it to prove the restore procedure. For self-hosted PostgreSQL, `scripts/backup-postgres.sh` and `scripts/restore-postgres.sh` are included as a baseline.

## Multi-instance note

The built-in rate limiter is intentionally simple and per application instance. If the API is scaled horizontally, replace it with a shared limiter (for example Redis or the hosting provider's edge rate limiting).
