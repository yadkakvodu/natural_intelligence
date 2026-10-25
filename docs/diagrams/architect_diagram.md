```mermaid
graph TD
    classDef frontend fill:#e1f5fe,stroke:#0277bd,stroke-width:2px;
    classDef java fill:#fff9c4,stroke:#fbc02d,stroke-width:2px;
    classDef python fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px;
    classDef db fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px;
    classDef external fill:#eceff1,stroke:#546e7a,stroke-width:2px;

    subgraph "Клиентский слой"
        FE[Фронтенд / ЛПР]:::frontend
    end

    subgraph "Java Orchestrator (API Gateway)"
        J_API["Spring Boot API
        GetAnalyst Standard"]:::java
        J_Gate["Budget Guard &
        Rate Limiter"]:::java
        J_Ctx["Context Builder
        (PromptBuilderService)"]:::java
        J_Trans["Response Transformer
        (Pagination, IDs)"]:::java
    end

    subgraph "Python AI Core (Executor)"
        P_API["FastAPI Internal
        /internal/v1/generate"]:::python
        P_RAG["RAG Service
        (Vector Search + Boosting)"]:::python
        P_LLM["LLM Executor
        (DeepSeek/Qwen)"]:::python
        P_Log["Async Logger
        (Traces & Messages)"]:::python
    end

    subgraph "Data Layer"
        DB[("PostgreSQL + pgvector
        6 Tables")]:::db
    end

    subgraph "External Services"
        LLM_API["Qwen3.6 / DeepSeek API"]:::external
    end

    FE -- "1. GET /news-feed" --> J_API
    FE -- "2. GET /insight" --> J_API
    FE -- "3. POST /chat message" --> J_API
    FE -- "4. GET /chat-history" --> J_API
    FE -- "5. GET /sources" --> J_API

    J_API --> J_Gate
    J_Gate -- "Pass" --> J_Ctx
    J_Gate -- "429 Too Many Requests" -.-> FE
    
    J_Ctx -- "SQL SELECT profile, history, news_meta" --> DB
    J_Ctx -- "Ready Prompt (Chat) / Params (Feed)" --> J_API
    
    J_API -- "HTTP Context + Profile + Ready Messages" --> P_API
    J_API -- "X-Trace-Id" --> P_API

    P_API -- "Request Type feed/insight/chat" --> P_RAG
    P_API -- "Request Type chat" --> P_LLM
    
    P_RAG -- "Vector Search + Rule Boosting" --> DB
    P_RAG -- "News Candidates" --> P_LLM
    
    P_LLM -- "Generate Content" --> LLM_API
    LLM_API -- "Response + Tokens" --> P_LLM
    
    P_LLM -- "Result + Metadata" --> P_API
    P_API -- "Async INSERT" --> P_Log
    P_Log -- "Save Trace & Messages" --> DB

    P_API -- "Raw JSON Result" --> J_Trans
    J_Trans -- "Transform Add IDs, Levels, Pagination" --> J_API
    J_API -- "200 OK GetAnalyst Response" --> FE

    subgraph "Ingest Pipeline (One-time)"
        CSV[CSV Files] --> PyETL[Python Ingest Script]
        PDF[Alfa Analytics PDF] --> PyETL
        PyETL -- "pandas / pypdf" --> PyETL
        PyETL -- "INSERT + Embeddings (bge-m3)" --> DB
    end
```
