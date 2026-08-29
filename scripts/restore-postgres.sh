#!/usr/bin/env sh
set -eu

: "${DATABASE_URL:?Set DATABASE_URL to the target PostgreSQL connection URI}"
: "${1:?Usage: restore-postgres.sh <backup.dump>}"

pg_restore \
  --clean \
  --if-exists \
  --no-owner \
  --no-acl \
  --dbname="$DATABASE_URL" \
  "$1"
