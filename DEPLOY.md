# Развертывание в Docker

Стек: **PostgreSQL 18 + pgvector** (БД из дампа), **Java 17 / Spring Boot 3.3** (backend),
**Python 3.11 / FastAPI** (AI-core). Всё поднимается одним `docker compose up`.

## Состав

| Сервис | Образ | Порт (host) | Данные |
|---|---|---|---|
| `db` | `ni-db:latest` (`pgvector/pgvector:pg18`) | `127.0.0.1:55432` | том `pgdata`, дамп восстанавливается при первом старте |
| `backend` | `ni-backend:latest` (Maven 3.9 → Temurin 17 JRE) | `8080` | — |
| `ai-core` | `ni-ai-core:latest` (Python 3.11, CPU-torch) | `8000` | том `hf_cache` (модель bge-m3 ~2.3 ГБ) |

### Файлы, которые нужны для развертывания

```
docker-compose.yml
.env.example                  # скопировать в .env
db/
  Dockerfile                  # pgvector/pgvector:pg18 + restore при первом старте
  backup.sql.gz               # исправленный дамп (UTF-8)
  init/00-roles.sql           # роли-владельцы объектов из дампа
  init/10-restore.sh          # gunzip | psql
  fix_backup.py               # восстановление битого дампа (UTF-16 + CP866)
backend-java/
  Dockerfile
  pom.xml                     # восстановлен (Spring Boot 3.3.5, Java 17)
  src/main/resources/application.properties
  src/main/java/...           # PythonClient читает PYTHON_AI_URL
ai-core-python/
  Dockerfile                  # CPU-torch + libgomp1
  requirements.txt            # sentence-transformers + hub/transformers запинены
  python_ai.py                # DATABASE_URL/PG_*, /health
docs/db/schema.sql, init.sql  # исторические, в compose больше не используются
```

Внешних артефактов нет: все образы собираются из репозитория, БД — из `db/backup.sql.gz`.

## Требования к хосту

- Docker Engine / Docker Desktop + Compose v2
- ~12 ГБ диска (образы ~3.5 ГБ + модель 2.3 ГБ + БД ~1 ГБ + запас)
- 4–6 ГБ RAM (ai-core с моделью ~2.5–3 ГБ)
- Сеть: Docker Hub (сборка), HuggingFace (первый старт ai-core), шлюз LLM (insight/chat)
- Для insight/chat — `DEEPSEEK_API_KEY` (feed работает без него)

## Быстрый старт

```bash
cp .env.example .env
# вписать DEEPSEEK_API_KEY=... (можно оставить пустым — тогда только feed)

docker compose up -d --build
docker compose ps                     # ждать (healthy) у всех трёх
```

Первый запуск: сборка ai-core ~3–5 мин, скачивание модели bge-m3 ~2–3 мин
(healthcheck ai-core имеет `start_period: 600s`, это нормально).

Открыть: <http://localhost:8080/news> (UI), API: `http://localhost:8080/api/...`.

### Порты на этой машине

На macOS уже заняты `8080` (локальная Java) и `5432/5433` (локальный PostgreSQL 17),
поэтому в примерах ниже переопределены порты:

```bash
BACKEND_PORT=18081 AI_CORE_PORT=18001 docker compose up -d
```

| Переменная | По умолчанию | Зачем |
|---|---|---|
| `BACKEND_PORT` | `8080` | порт UI/API на хосте |
| `AI_CORE_PORT` | `8000` | порт AI-core на хосте (для отладки) |
| `DB_HOST_PORT` | `55432` | порт БД на хосте (внутри compose — `db:5432`) |
| `POSTGRES_USER/PASSWORD/DB` | `user/password/natural_intelligence` | креды БД для всех сервисов |
| `DEEPSEEK_API_KEY` | — | LLM для insight/chat |
| `GATEWAY_BASE_URL`, `MODEL_NAME` | шлюз хакатона | LLM-шлюз |

## Проверка

```bash
curl -s localhost:8080/actuator/health          # {"status":"UP"}  (включая БД)
curl -s localhost:8000/health                   # {"status":"ok"}
curl -s "localhost:8080/api/clients/U_80/news-feed?limit=2"   # JSON с items
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/news  # 200
```

БД:

```bash
psql -h 127.0.0.1 -p 55432 -U user -d natural_intelligence
# пароль password; 85 514 clients, 389 396 платежей, 9 news
```

Ожидаемое поведение:

- `feed` работает без `DEEPSEEK_API_KEY` (векторный поиск + шаблонные инсайты);
- `insight` и `chat` требуют ключ, иначе 500 от AI-core;
- если ai-core ещё грузит модель — backend вернёт
  `{"errorMessage":"Сервис генерации временно недоступен..."}` (это не падение).

## Обновление / пересборка

```bash
docker compose build --pull          # пересобрать образы
docker compose up -d                 # перезапустить изменённые
docker compose logs -f ai-core       # смотреть загрузку модели
```

Полный сброс (включая данные БД и кэш модели):

```bash
docker compose down -v
docker compose up -d --build
```

## Типовые проблемы

| Симптом | Причина / решение |
|---|---|
| `port is already allocated` | занят `8080`/`8000`/`5432` — задать `BACKEND_PORT` / `AI_CORE_PORT` / `DB_HOST_PORT` |
| ai-core долго `starting` | качается bge-m3 (~2.3 ГБ); проверить `docker compose logs -f ai-core` и доступ к huggingface.co |
| ai-core перезапускается | нет доступа к HF или мало RAM; кэш — том `hf_cache` |
| backend 500 «Сервис генерации недоступен» | ai-core ещё не healthy либо упал: `docker compose ps`, логи |
| insight/chat 500 | не задан `DEEPSEEK_API_KEY` в `.env` |
| `psql: invalid byte sequence` при restore | дамп не в UTF-8: `python3 db/fix_backup.py broken.sql fixed.sql` |
| `Container name "/ni_database" is already in use` | старый контейнер с прошлого запуска: `docker rm -f ni_database` (том не удаляется) |
| пустая БД после `down` | том удалён (`down -v`); при следующем старте дамп восстановится автоматически |

## Что было исправлено для запуска

1. `backend-java/pom.xml` — вместо macOS `.DS_Store` восстановлен рабочий POM
   (Spring Boot 3.3.5, Java 17, web/data-jpa/thymeleaf/actuator/postgres).
2. `backend-java/mvnw` — перегенерирован Maven Wrapper (раньше содержал XML-POM);
   Dockerfile использует `mvn` из образа `maven:3.9`.
3. `eclipse-temurin:17-jre-alpine` не имеет linux/arm64 → заменён на Debian JRE.
4. `PythonClient` читает `PYTHON_AI_URL` (раньше был захардкожен `localhost:8000`).
5. AI-core читает `DATABASE_URL`/`PG_*` и `PG_SSLMODE` (раньше — только PG_* с `sslmode=require`).
6. `huggingface-hub==0.25.2` + `transformers==4.44.2` — иначе `sentence-transformers 2.2.2`
   падает на импорте (`cached_download` удалён в hub 0.26).
7. CPU-сборка torch: образ AI-core 9.8 ГБ → 2.2 ГБ.
8. PostgreSQL 18: `PGDATA=/var/lib/postgresql/18/docker`, поэтому том монтируется
   в `/var/lib/postgresql` (старый путь `.../data` не работает).
9. Дамп `backup.sql`: был UTF-16LE + BOM + mojibake CP866 (PowerShell-редирект),
   восстановлен lossless; проверено восстановлением в PG 18.6 без ошибок.

## Проверено фактически

- `db`: healthy, дамп восстановлен, 85 514 clients / 389 396 payments / 9 news,
  pgvector 0.8.7, similarity-поиск работает, рестарт не перезаливает данные.
- `backend`: healthy, `/actuator/health` = `{"status":"UP"}` (БД), `/news` = 200.
- `ai-core`: healthy, модель bge-m3 загружена, `/health` = `{"status":"ok"}`.
- Сквозной запрос `GET /api/clients/U_80/news-feed?limit=2` → JSON с `items`
  (backend → ai-core → pgvector).
