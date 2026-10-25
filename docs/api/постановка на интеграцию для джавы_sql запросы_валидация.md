Ниже представлена обновленная, финальная постановка для Java-разработчика. Документ очищен от упоминаний стандарта GetAnalyst и внешних ссылок, полностью синхронизирован с текущей архитектурой (включая исправления чата) и содержит только актуальные технические требования.

---

# 📄 ПОСТАНОВКА НА РАЗРАБОТКУ: Java API Gateway & Orchestrator

**Проект:** Альфа Хак_Новостная аналитика  
**Роль компонента:** Публичный API Gateway, Оркестратор бизнес-логики, Страж бюджета  
**Версия документа:** 3.0 (Final)  
**Связанные документы:** OpenAPI Spec v3.0, DDL v6, Internal Contract Spec  

## 1. Концептуальная роль Java
Java является **единой точкой входа** и **оркестратором**. Она НЕ занимается генерацией контента, векторным поиском или LLM. Её обязанности:
1.  Прием внешних запросов по утвержденному OpenAPI контракту.
2.  Валидация, Rate Limiting и контроль бюджета токенов.
3.  Сбор и подготовка контекста (Client Profile) из БД.
4.  **Для `/chat`:** Самостоятельная сборка полного промпта (история + мета-новость + профиль).
5.  Вызов внутреннего Python AI Service через REST.
6.  Трансформация «сырого» ответа Python в публичный контракт API.
7.  Сквозная трассировка (`X-Trace-Id`).

> ⚠️ **Критическое правило:** Java никогда не передает сырые данные пользователя в Python. Для `/feed` и `/insight` Java передает нормализованный профиль и параметры. Для `/chat` Java передает готовый массив `messages`. Java никогда не отдает пользователю ответ от Python без обертки и трансформации.

---

## 2. Управление бюджетом и лимитами (Budget Guard)
Реализовать двойную защиту ДО вызова Python. Это зона ответственности Java.

### 2.1. Rate Limiting (RPS)
*   **Лимиты:** 
    *   `/news-feed`: 30 rpm
    *   `/insight`: 20 rpm
    *   `/chat`: 20 rpm
*   **Механизм:** In-memory счетчик (Sliding Window) или Redis. Для хакатона допустим in-memory.
*   **Действие:** При превышении → `HTTP 429 Too Many Requests`. Тело: `{"errorMessage": "Превышен лимит запросов..."}`.
*   **Проверка:** Строго при каждом входящем HTTP-запросе.

### 2.2. Контроль бюджета токенов
Python лишь отчитывается о расходе. Java принимает решение «пускать или нет».

| Этап | Действие Java | Источник данных |
| :--- | :--- | :--- |
| **Startup Hook** | Выполнить `SELECT SUM(tokens_input + tokens_output) FROM agent_traces WHERE created_at > NOW() - INTERVAL '24h'`. Инициализировать локальный счетчик `budgetCounter`. | PostgreSQL `agent_traces` |
| **Pre-check** | Перед вызовом Python: если `(budgetCounter + прогноз_расхода) > ЛИМИТ` → вернуть 429. Прогноз: Feed=2000 ток., Insight=5000 ток., Chat=3000 ток. | Локальная переменная / Redis |
| **Post-update** | После ответа Python: добавить фактические `tokens_in + tokens_out` в `budgetCounter`. | Response body Python |
| **Sync Job** | Раз в 5 минут синхронизировать локальный счетчик с БД для защиты от рассинхрона. | PostgreSQL `agent_traces` |

---

## 3. Подготовка контекста (Context Builder)
Java собирает профиль клиента из БД и маппит его во внутренний DTO. Передаются ТОЛЬКО семантически значимые поля.

### SQL-запрос (`ClientProfileRepository`)
```sql
SELECT 
    synthetic_u,
    mainokved_code,
    mainokved_name,
    COALESCE(cl_product_loa_mark, false) as has_credits,
    COALESCE(fts_flag, false) as has_ved,
    fts_country_name,
    revenue_company,
    group_id
FROM public.clients 
WHERE synthetic_u = :syntheticU;
```

### Внутренний DTO (только 7 полей!)
Остальные поля из CSV (`org_type`, `okfs_name`, `amount_workers_*` и др.) **НЕ передаются** в Python.
```json
{
  "mainokved_code": "25.61",
  "mainokved_name": "Металлообработка",
  "has_credits": true,
  "has_ved": true,
  "ved_country": "КИТАЙ",
  "revenue": 1400000000,
  "group_id": "group_134"
}
```

---

## 4. Валидация входных данных
Отсекать некорректные запросы до обращения к БД и Python.

| Параметр | Правило валидации | Код ошибки |
| :--- | :--- | :--- |
| `synthetic_u` (Path) | Regex `^U_[A-Z0-9]+$` | `400 Bad Request` |
| `limit` (Query) | Integer, 1 ≤ limit ≤ 20 (для feed), 1 ≤ limit ≤ 50 (для chat-history) | `400 Bad Request` |
| `offset` (Query) | Integer, offset ≥ 0 | `400 Bad Request` |
| `order` (Query) | Whitelist: `published_at,desc`, `relevance_score,desc` | `400 Bad Request` |
| `topic` (Query) | Whitelist: `cbr_rate`, `ved_china`, `oil`, `macro`, `taxes` | `400 Bad Request` |
| `is_analytics_only` | Boolean | `400 Bad Request` |
| `news_id` (Path) | Не пустая строка, формат `^[a-z0-9-]+$` | `400 Bad Request` |
| `message` (Chat Body) | String, длина 1–1000 символов | `400 Bad Request` |

---

## 5. Интеграция с Python (Internal Client)
Вызов внутреннего сервиса через REST over HTTP.

### Контракт вызова
*   **URL:** `POST http://python-agent:8000/internal/v1/generate`
*   **Headers:** Обязательно передавать `X-Trace-Id: <UUID>` для сквозной трассировки.
*   **Timeouts:** Connect: 3 сек, Read: 15 сек.
*   **Body:**
    ```json
    {
      "request_type": "feed" | "insight" | "chat",
      "synthetic_u": "U_80",
      "client_profile": { /* 7 полей из п.3 */ },
      "params": {
        "news_id": "alfa-macro-2026-09-07", // Только для insight
        "limit": 5,                         // Только для feed
        "topic_filter": "cbr_rate",         // Только для feed
        "is_analytics_only": false          // Только для feed
      },
      "llm_request": {                      // ТОЛЬКО для chat
        "messages": [ /* Готовый массив от Java */ ],
        "temperature": 0.3,
        "max_tokens": 500
      }
    }
    ```

### Обработка ошибок Python
| Ответ Python | Действие Java | Сообщение пользователю |
| :--- | :--- | :--- |
| `200 OK` | Трансформировать и отдать | Стандартный ответ API |
| `400 Bad Request` | Логировать как баг интеграции | `500 Internal Server Error` |
| `429 Too Many Requests` | Пробросить или заменить на свой | `429 Too Many Requests` |
| `503 Service Unavailable` | Пробросить | `503 Service Unavailable` («Сервис генерации временно недоступен») |
| Timeout / Connection Refused | Вернуть 503 | `503 Service Unavailable` |

---

## 6. Специфика метода `/chat` (Prompt Assembly)
**ВАЖНО:** Для чата Java самостоятельно формирует промпт. Python НЕ имеет доступа к БД для чтения истории.

### Алгоритм сборки `llm_request.messages`
1.  Загрузить историю: `SELECT role, content FROM messages WHERE conversation_id = '{synthetic_u}_{news_id}' ORDER BY created_at ASC LIMIT 10`.
2.  Загрузить мета-новости: `SELECT title, summary FROM news WHERE news_id = :newsId`.
3.  Сформировать System Prompt: `"Ты финансовый советник... Новость: '{title}'. Клиент: {industry}, кредиты={has_credits}..."`.
4.  Собрать массив: `[System, ...History(10), UserMessage]`.
5.  Передать этот массив в поле `llm_request.messages`.

---

## 7. Трансформация ответа Python → Public API (CRITICAL)
Python возвращает «полуфабрикат». Java обязана выполнить следующие действия:

### 7.1. Генерация мета-полей (для каждого элемента items)
| Поле | Действие Java | Логика |
| :--- | :--- | :--- |
| `insight_id` / `message_id` | **Сгенерировать** | UUID v4. Python НЕ возвращает эти ID. |
| `relevance_level` | **Вычислить** | На основе `relevance_score` от Python:<br>• > 0.8 → `"HIGH"`<br>• > 0.5 → `"MEDIUM"`<br>• ≤ 0.5 → `"LOW"` |
| `title` / `summary` | Оставить как есть | Python уже персонализировал заголовок и сгенерировал summary. |
| `semantic_tags` | Валидировать | Проверить сумму `influence_weight` ≈ 1.0. Если нет — нормализовать. |

### 7.2. Формирование пагинации
Python возвращает `total_found` (общее число кандидатов). Java рассчитывает параметры страницы:
```java
int count = Math.min(items.size(), limit); // Реальное кол-во элементов на текущей странице
// Важно: Java сама режет массив items по limit/offset после получения полного списка от Python!
PaginationDTO pagination = new PaginationDTO(limit, offset, totalFoundFromPython);
```

### 7.3. Сортировка и Фильтрация
*   **По умолчанию:** Использовать порядок от Python (уже отсортирован по `relevance_score DESC` с бустингом).
*   **При явном запросе `order=published_at,desc`:** Java сортирует массив `items` локально по полю `published_at` ПОСЛЕ получения ответа.
*   **Запрет на пост-фильтрацию:** Если Java передала `params.topic_filter`, она ОБЯЗАНА доверять результату Python. Повторная фильтрация массива `items` в Java запрещена.

---

## 8. Сквозная трассировка
1.  При входе в контроллер сгенерировать `traceId = UUID.randomUUID()`.
2.  Передать его в заголовке `X-Trace-Id` при вызове Python.
3.  Записать `traceId` в логи MDC для всех последующих операций (БД, метрики).
4.  Этот ID должен совпадать с тем, что Python запишет в поле `trace_id` таблицы `agent_traces`.

---

## 9. Метрики и мониторинг
Реализовать экспорт следующих метрик (Prometheus/Micrometer):
*   `news_feed_requests_total` / `news_insights_requests_total` / `chat_requests_total` — счетчики запросов.
*   `generation_duration_seconds` — время генерации (p50, p95, p99). Берется из поля `processing_time_ms` ответа Python.
*   `llm_tokens_consumed_total` — сумма токенов из ответов Python.
*   `api_request_errors_total` — ошибки по типам (4xx, 5xx).
*   `avg_relevance_score` — средняя релевантность (для алерта деградации качества).

---

## 10. Чек-лист готовности (Definition of Done)

- [ ]  Реализован `BudgetGuardService` с инициализацией при старте и синхронизацией раз в 5 мин.
- [ ]  Настроен Rate Limiter (30 rpm feed, 20 rpm insight/chat).
- [ ]  Создан `ClientProfileRepository` с маппингом только 7 семантических полей.
- [ ]  Реализована валидация всех query/path параметров согласно таблице в п.4.
- [ ]  Настроен REST-клиент для Python с таймаутами (Connect 3s, Read 15s) и заголовком `X-Trace-Id`.
- [ ]  **Для `/chat`:** Реализован сервис сборки промпта (SQL история + мета-новость + профиль → массив messages).
- [ ]  Реализована генерация `insight_id`/`message_id` и вычисление `relevance_level` в слое трансформации.
- [ ]  Пагинация применяется ТОЛЬКО в Java к полному списку от Python.
- [ ]  Параметр `order` обрабатывается локальной сортировкой в Java.
- [ ]  Отсутствует повторная фильтрация `items` по `topic` в Java.
- [ ]  Ответ обернут в структуру `{ "pagination": {...}, "items": [...] }` (для feed/history).
- [ ]  Ошибки Python корректно маппятся в HTTP-статусы API.
- [ ]  Экспортируются все метрики из раздела 9.
- [ ]  Написаны unit-тесты на трансформацию ответа, Budget Guard и сборку промпта для чата.