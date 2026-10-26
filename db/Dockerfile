# PostgreSQL 18 + pgvector with the project database pre-loaded from backup.sql.gz.
# The dump is restored on the first start (empty PGDATA); data then lives in the pgdata volume.
FROM pgvector/pgvector:pg18

# Dev defaults; override via docker-compose / .env in real deployments.
ENV POSTGRES_USER=user \
    POSTGRES_PASSWORD=password \
    POSTGRES_DB=natural_intelligence

# Roles referenced as object owners inside the dump must exist before the restore.
COPY init/00-roles.sql /docker-entrypoint-initdb.d/00-roles.sql
COPY init/10-restore.sh /docker-entrypoint-initdb.d/10-restore.sh
COPY backup.sql.gz /opt/backup.sql.gz

RUN chmod 0644 /docker-entrypoint-initdb.d/00-roles.sql /opt/backup.sql.gz \
 && chmod 0755 /docker-entrypoint-initdb.d/10-restore.sh

EXPOSE 5432

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
    CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1
