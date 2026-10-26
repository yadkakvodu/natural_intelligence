#!/bin/bash
set -euo pipefail

echo "[db-init] restoring /opt/backup.sql.gz into ${POSTGRES_DB} ..."

gunzip -c /opt/backup.sql.gz | psql -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB"

echo "[db-init] restore complete"
