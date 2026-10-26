# DB container: PostgreSQL 18 + pgvector

Образ собирается из `Dockerfile` и содержит исправленный дамп `backup.sql.gz`.
При первом старте с пустым томом `docker-entrypoint`:

1. создаёт роли-владельцы из дампа (`init/00-roles.sql`);
2. восстанавливает дамп (`init/10-restore.sh`).

## Запуск

```bash
docker compose up -d --build db
docker compose ps            # ждём (healthy)
```

Подключение:

```bash
# с хоста (5432/5433 на этой машине заняты локальным PostgreSQL 17)
psql -h 127.0.0.1 -p 55432 -U user -d natural_intelligence

# из контейнеров compose
postgresql://user:password@db:5432/natural_intelligence
```

Порт хоста можно переопределить: `DB_HOST_PORT=5432 docker compose up -d db`.

## Данные

- креды по умолчанию: `user` / `password` / `natural_intelligence`;
- том `pgdata` монтируется в `/var/lib/postgresql` — в образе PostgreSQL 18
  `PGDATA` лежит в `/var/lib/postgresql/18/docker`, а не в `.../data`;
- 85 514 клиентов, 389 396 платежей, 9 новостей, pgvector 0.8.7.

## Пересборка с нуля

```bash
docker compose rm -sf db
docker volume rm natural_intelligence_pgdata
docker compose up -d --build db
```

## Про дамп

`backup.sql.gz` — восстановленный `backup.sql`: исходник был испорчен перенаправлением
вывода в Windows PowerShell (UTF-16LE + BOM + mojibake CP866). Пересобрать из битого файла:

```bash
python3 db/fix_backup.py ~/Downloads/backup.sql /tmp/backup_fixed.sql
gzip -9 -c /tmp/backup_fixed.sql > db/backup.sql.gz
```
