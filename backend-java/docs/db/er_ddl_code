-- ==========================================
-- 1. ИНИЦИАЛИЗАЦИЯ РАСШИРЕНИЙ
-- ==========================================
CREATE EXTENSION IF NOT EXISTS pgvector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- 2. ТАБЛИЦА КЛИЕНТОВ (public.clients)
-- Хранит профили компаний для персонализации
-- ==========================================
CREATE TABLE public.clients (
    synthetic_u VARCHAR(50) PRIMARY KEY,
    
    -- Семантические признаки (используются в RAG и промптах)
    mainokved_code VARCHAR(10),
    mainokved_name VARCHAR(255),
    cl_product_loa_mark BOOLEAN DEFAULT FALSE, -- Наличие кредитов
    fts_flag BOOLEAN DEFAULT FALSE,            -- Флаг ВЭД
    fts_country_name VARCHAR(100),             -- Страна ВЭД (например, 'КИТАЙ')
    revenue_company NUMERIC(18, 2),            -- Выручка
    group_id VARCHAR(50),                      -- ID группы компаний
    
    -- Остальные поля из CSV (хранятся, но не передаются в AI Core)
    org_type VARCHAR(50),
    okfs_name VARCHAR(255),
    amount_workers_gradation VARCHAR(50),
    company_age_years_gradation VARCHAR(50),
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_clients_synthetic_u ON public.clients(synthetic_u);

-- ==========================================
-- 3. ТАБЛИЦА НОВОСТЕЙ (public.news)
-- Основной контент для RAG-поиска
-- ==========================================
CREATE TABLE public.news (
    news_id VARCHAR(100) PRIMARY KEY,
    
    title TEXT NOT NULL,
    summary TEXT,                -- Краткая выжимка (для чата и ленты)
    body_md TEXT,                -- Полный текст в Markdown (для глубокого инсайта)
    
    published_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(255),         -- Название источника
    is_analytics BOOLEAN DEFAULT FALSE, -- Флаг аналитики Альфа-Банка
    
    topics TEXT[],               -- Массив тем: ['cbr_rate', 'ved_china', 'oil']
    
    embedding VECTOR(1024),      -- Векторное представление (модель bge-m3)
    content_hash VARCHAR(64),    -- Хеш для дедупликации
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Индекс HNSW для быстрого векторного поиска
CREATE INDEX idx_news_embedding ON public.news 
USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_news_published_at ON public.news(published_at);
CREATE INDEX idx_news_topics ON public.news USING GIN (topics);

-- ==========================================
-- 4. ТАБЛИЦА ТРЕЙСОВ АГЕНТА (public.agent_traces)
-- Для трассируемости (Критерий №9) и контроля бюджета (Критерий №10)
-- ==========================================
CREATE TABLE public.agent_traces (
    trace_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    synthetic_u VARCHAR(50) REFERENCES public.clients(synthetic_u),
    news_id VARCHAR(100),        -- Может быть NULL для общих запросов
    
    request_type VARCHAR(20) CHECK (request_type IN ('feed', 'insight', 'chat')),
    
    steps JSONB,                 -- Логика принятия решений агентом
    llm_model_used VARCHAR(50),  -- Фактическая модель (deepseek/qwen)
    
    tokens_input INTEGER DEFAULT 0,
    tokens_output INTEGER DEFAULT 0,
    
    relevance_score NUMERIC(4, 3), -- Итоговый скор релевантности
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_traces_synthetic_u ON public.agent_traces(synthetic_u);
CREATE INDEX idx_traces_created_at ON public.agent_traces(created_at);
CREATE INDEX idx_traces_request_type ON public.agent_traces(request_type);

-- ==========================================
-- 5. ТАБЛИЦА СООБЩЕНИЙ ЧАТА (public.messages)
-- Для хранения истории диалогов (Критерий №11)
-- ==========================================
CREATE TABLE public.messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    conversation_id VARCHAR(150) NOT NULL, -- Формат: '{synthetic_u}_{news_id}'
    
    role VARCHAR(10) CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    
    citations JSONB,             -- Источники, на которые сослался ассистент
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Составной индекс для быстрой загрузки истории чата
CREATE INDEX idx_messages_conv_id_created ON public.messages(conversation_id, created_at ASC);

-- ==========================================
-- 6. ДОПОЛНИТЕЛЬНЫЕ КОММЕНТАРИИ
-- ==========================================
COMMENT ON TABLE public.clients IS 'Профили корпоративных клиентов для персонализации';
COMMENT ON TABLE public.news IS 'База новостей и аналитики с векторными эмбеддингами';
COMMENT ON TABLE public.agent_traces IS 'Логи работы AI-агента для аудита и контроля бюджета';
COMMENT ON TABLE public.messages IS 'История переписки пользователей с AI-ассистентом';
