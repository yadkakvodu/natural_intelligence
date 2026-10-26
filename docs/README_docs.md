# Natural Intelligence — структура проекта

```text
natural_intellegence/
├── README.md
├── THIRD_PARTY_NOTICES.md
├── docker-compose.yml
├── backend-java/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/       # контроллеры, сервисы, DTO, entity, repository
│       ├── main/resources/  # templates/index.html, static/{script.js,style.css,assets}
│       └── test/java/
├── ai-core-python/
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── python_ai.py         # POST /internal/v1/generate
│   └── pdftodb.py           # ETL PDF → pgvector
└── docs/
    ├── README_docs.md
    ├── architecture.md
    ├── api/
    │   ├── openapi.yaml
    │   ├── api_methods.md
    │   └── intagration_java_python.md
    ├── db/
    │   ├── schema.sql
    │   ├── init.sql
    │   └── ER_diagram.png
    └── diagrams/
        ├── c1.puml
        └── c3.puml
```

## Назначение

| Путь | Назначение |
|---|---|
| `backend-java/` | Java Orchestrator: REST API, валидация, бюджет, сборка промпта, трансформация |
| `ai-core-python/` | Python AI Core: RAG, boosting, вызов LLM, трассировка |
| `docs/architecture.md` | Архитектура и внутренний контракт `/internal/v1/generate` |
| `docs/api/` | OpenAPI и постановки по 5 публичным методам |
| `docs/db/` | DDL, тестовые данные, ER-диаграмма |
| `docs/diagrams/` | PlantUML-исходники C1 и C3 |
