# Архитектура системы «Новостной консультант Альфа-Банка»

## 1. Общая структура

Система построена по паттерну **Orchestrator-Executor**, где:
- **Java Spring Boot** выступает в роли оркестратора (API Gateway, валидация, контроль бюджета, трансформация ответов в стандарт GetAnalyst).
- **Python FastAPI** выполняет роль интеллектуального исполнителя (RAG, генерация через LLM, асинхронная трассировка).


┌─────────────     ┌──────────────────┐     ┌─────────────────┐
│  Frontend   │────▶│  Java Spring Boot│────▶│  Python FastAPI │
│  (UI/API)   │◀────│  (Orchestrator)  │◀────│  (AI Executor)  │
└─────────────┘     └────────┬─────────┘     └────────────────┘
                             │                        │
                             ▼                        ▼
                      ┌─────────────┐          ┌─────────────┐
                      │ PostgreSQL  │◀────────▶│  LLM API    │
                      │ + pgvector  │          │ (DeepSeek,  │
                      └─────────────┘          │  Qwen)      │
                                               └─────────────┘


## 2. Компоненты и зоны ответственности

### 2.1 Java Orchestrator (Port 8080)
Java является единой точкой входа и **не содержит бизнес-логики генерации**.
**Ответственность:**
- Валидация входных параметров (`synthetic_u`, `news_id`) и проверка существования сущностей в БД.
- Rate Limiting (30 req/min для feed, 20 req/min для insight/chat).
- Контроль бюджета LLM (Pre-check перед вызовом Python, Post-update после ответа). Лимит: 500 руб./сутки.
- Сборка контекста для чата: загрузка истории из БД, мета-данных новости и формирование массива `messages`.
- Трансформация «сырых» ответов Python в формат GetAnalyst (добавление `insight_id`, расчет `relevance_level`, пагинация).

### 2.2 Python AI Core (Port 8000)
Python является «интеллектуальным ядром». Он **не знает** о публичном API, пагинации и HTTP-статусах.
**Ответственность:**
- Векторный поиск (RAG) через `pgvector` с фильтрацией по дате (30 дней).
- Rule-based бустинг релевантности на основе профиля клиента.
- Генерация контента через LLM (Feed Summary / Deep Insight / Chat).
- Асинхронная запись трейсов в `agent_traces` и истории в `messages`.
- Возврат «сырого» результата генерации без API-оберток.

### 2.3 PostgreSQL + pgvector
**Таблицы:**
- `clients`: профили компаний (7 семантических признаков).
- `news`: новости с векторными эмбеддингами (1024d, модель `bge-m3`).
- `messages`: история чата (JSONB для citations).
- `agent_traces`: полная трассировка действий AI (шаги, токены, модель).

## 3. Внутренний контракт (Java ↔ Python)

Вся коммуникация между сервисами проходит через единый эндпоинт.

### Эндпоинт
`POST /internal/v1/generate`

### Заголовки
- `Content-Type`: application/json
- `X-Trace-Id`: UUID (Обязателен для сквозной трассировки).

### Тело запроса
```json
{
  "request_type": "feed" | "insight" | "chat",
  "synthetic_u": "U_80",
  "client_profile": {
    "mainokved_code": "25.61",
    "has_credits": true,
    "has_ved": true,
    "ved_country": "КИТАЙ",
    "revenue": 1400000000
  },
  "params": { ... },
  "llm_request": { ... }
}
```

### Специфика полей по типам запросов
| Поле | feed | insight | chat | Комментарий |
|------|------|---------|------|-------------|
| `params.limit` | ✅ | ❌ | ❌ | Кол-во кандидатов для RAG. |
| `params.news_id` | ❌ | ✅ |  | ID новости для глубокого анализа. |
| `llm_request.messages`| ❌ |  | ✅ | Готовый массив сообщений (собирается Java). |

## 4. Ключевые алгоритмы

### 4.1 RAG Search с Rule-Based Boosting
1. **Фильтрация:** Жесткое условие `WHERE published_at > NOW() - INTERVAL '30 days'`.
2. **Векторный поиск:** SQL-оператор `<=>` (косинусное расстояние).
3. **Бустинг (Python):**
   - Если `has_credits == true` AND `'cbr_rate' in topics` → `score += 0.25`.
   - Если `has_ved == true` AND `ved_country == 'КИТАЙ'` AND `'ved_china' in topics` → `score += 0.20`.
4. **Отбор:** Сортировка по итоговому скорy, обрезка до top-K.

### 4.2 Контроль бюджета LLM (Java)
- **Pre-check:** Прогноз расхода перед вызовом Python (Feed ≈ 2000 ток., Insight ≈ 5000 ток.). Если `(current + estimate) > 500 руб.` → возврат 429.
- **Post-update:** Фактический расход из ответа Python (`tokens_used`) добавляется в локальный счетчик и синхронизируется с БД каждые 5 минут.

### 4.3 Асинхронная трассировка (Python)
Запись в БД выполняется через `asyncio.to_thread`, чтобы не увеличивать latency ответа.
- В `agent_traces` сохраняется JSON-массив шагов (`RAG_Search`, `LLM_Call`, `Self_Check`), использованная модель и количество токенов.

## 5. Flow данных (End-to-End)

### 5.1 Лента новостей (GET /news-feed)
1. Client → Java API.
2. Java → Валидация, Rate Limit, загрузка профиля.
3. Java → `POST /internal/v1/generate` (`request_type=feed`).
4. Python → RAG Search + Boosting → LLM Call (легкая модель для summary).
5. Python → Async Write `agent_traces` → Return raw JSON.
6. Java → Добавление `insight_id`, `relevance_level`, пагинация → Return to Client.

### 5.2 Детальный инсайт (GET /insight)
1. Client → Java API.
2. Java → Валидация, загрузка `body_md` новости.
3. Java → `POST /internal/v1/generate` (`request_type=insight`).
4. Python → LLM Call (тяжелая модель, строгий JSON) → Self-Correction при ошибке парсинга.
5. Python → Async Write `agent_traces` → Return structured insight.
6. Java → Трансформация в GetAnalyst → Return to Client.

### 5.3 Чат (POST /chat)
1. Client → Java API (отправляет `message`).
2. Java → Загрузка последних 10 сообщений из `messages` + мета-данных новости.
3. Java → Сборка массива: `[System(meta_news + profile), ...History, UserMessage]`.
4. Java → `POST /internal/v1/generate` (передает готовый `llm_request.messages`).
5. Python → Вызов LLM с готовым массивом → Парсинг citations.
6. Python → Async Write user+assistant в `messages` → Return response.
7. Java → Return to Client.

## 6. Масштабируемость и безопасность

### Масштабируемость
- **Java и Python:** Stateless, масштабируются горизонтально через Kubernetes HPA.
- **PostgreSQL:** Поддержка Read Replicas для RAG-поиска.
- **Новые отрасли:** Добавляются через конфигурацию правил бустинга в Python без изменения API.

### Безопасность AI-компонентов
- Валидация `synthetic_u` (regex `^U_[A-Z0-9]+$`).
- Проверка существования клиента/новости **ДО** вызова Python.
- Изоляция диалогов по составному ключу `conversation_id`.
- В боевой системе: JWT-токен и ACL (в демо-стенде отключено).

## 7. Мониторинг и алерты

### Метрики (Prometheus)
- `news_feed_requests_total`, `chat_requests_total`
- `news_feed_generation_duration_seconds{quantile="0.95"}`
- `llm_tokens_consumed_total{model="deepseek-chat"}`
- `avg_relevance_score`

### Алерты (AlertManager)
- `LLMBudgetWarning`: расход токенов > 80% от дневного лимита.
- `HighLatency`: p95 latency > 5 сек.
- `LowRelevance`: средняя релевантность < 0.4 за час.

## 8. Технологический стек

| Компонент | Технология | Версия |
|-----------|-----------|--------|
| Backend Orchestrator | Java + Spring Boot | 17+, 3.x |
| AI Core Executor | Python + FastAPI | 3.10+, 0.104+ |
| Database | PostgreSQL + pgvector | 16+, 0.5+ |
| LLM Primary | DeepSeek-V4.1-Flash | API |
| LLM Fallback | Qwen3.6 35B A3B | API |
| Embeddings | BAAI/bge-m3 | 1024d |
| Infrastructure | Docker + Docker Compose | 2.x+ |
```

### Как добавить в репозиторий:
1. Создай файл `architecture.md` внутри папки `docs/`.
2. Вставь туда весь текст выше.
3. Закоммить и запушь:
```bash
git add docs/architecture.md
git commit -m "docs: add detailed architecture and internal contract specs"
git push
```
