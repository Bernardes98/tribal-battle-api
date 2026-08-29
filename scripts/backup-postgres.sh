#!/usr/bin/env sh
set -eu

: "${DATABASE_URL:?Set DATABASE_URL to a PostgreSQL connection URI}"

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUTPUT="${BACKUP_DIR}/tribal-battle-${TIMESTAMP}.dump"

mkdir -p "$BACKUP_DIR"
pg_dump --format=custom --no-owner --no-acl --dbname="$DATABASE_URL" --file="$OUTPUT"
find "$BACKUP_DIR" -type f -name 'tribal-battle-*.dump' -mtime "+$RETENTION_DAYS" -delete

echo "$OUTPUT"
