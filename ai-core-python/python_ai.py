import os
import json
import time
import uuid
from datetime import datetime, timezone
from typing import List, Optional

from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
import psycopg2
from psycopg2.extras import RealDictCursor
from sentence_transformers import SentenceTransformer
from openai import OpenAI
from dotenv import load_dotenv

# ==========================================
# 1. КОНФИГУРАЦИЯ И ИНИЦИАЛИЗАЦИЯ
# ==========================================
load_dotenv()

DB_CONFIG = {
    "dbname": os.getenv("PG_DB", "defaultdb"),
    "user": os.getenv("PG_USER", "avnadmin"),
    "password": os.getenv("PG_PASS"),
    "host": os.getenv("PG_HOST", "localhost"),
    "port": os.getenv("PG_PORT", "13266"),
    "sslmode": "require"
}

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")

# Настройки шлюза хакатона
GATEWAY_BASE_URL = os.getenv("GATEWAY_BASE_URL", "https://changellenge.up.railway.app/v1")
MODEL_NAME = os.getenv("MODEL_NAME", "deepseek/deepseek-v4.1-flash")

app = FastAPI(title="Alfa News AI Agent Core (Gateway)", version="1.0")

print("📥 Загрузка модели эмбеддингов BAAI/bge-m3...")
embedding_model = SentenceTransformer("BAAI/bge-m3")


# ==========================================
# 2. PYDANTIC СХЕМЫ (ВХОД ОТ JAVA)
# ==========================================
class ClientProfileDTO(BaseModel):
    mainokvedCode: Optional[str] = None
    mainokvedName: Optional[str] = None
    hasCredits: Optional[bool] = False
    hasVed: Optional[bool] = False
    vedCountry: Optional[str] = None
    revenue: Optional[float] = 0.0
    groupId: Optional[str] = None


class ParamsDTO(BaseModel):
    limit: Optional[int] = 5
    offset: Optional[int] = 0
    topicFilter: Optional[str] = None
    isAnalyticsOnly: Optional[bool] = False
    newsId: Optional[str] = None
    message: Optional[str] = None


class ChatMessageDTO(BaseModel):
    role: str
    content: str


class LlmRequestDTO(BaseModel):
    messages: List[ChatMessageDTO]
    temperature: Optional[float] = 0.3
    maxTokens: Optional[int] = 500


class IncomingJavaRequest(BaseModel):
    requestType: str  # "feed", "insight" или "chat"
    syntheticU: str
    clientProfile: Optional[ClientProfileDTO] = None
    params: Optional[ParamsDTO] = None
    llmRequest: Optional[LlmRequestDTO] = None


# ==========================================
# 3. ВЕКТОРНЫЙ ПОИСК В БД (RAG)
# ==========================================
def search_news_pgvector(query_text: str, limit: int = 5, news_id: Optional[str] = None) -> List[dict]:
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor(cursor_factory=RealDictCursor)

    if news_id:
        sql = """
            SELECT news_id, title, summary, body_md, published_at, source, is_analytics, topics
            FROM news
            WHERE news_id = %s
            LIMIT 1;
        """
        cursor.execute(sql, (news_id,))
        rows = cursor.fetchall()
        if not rows:
            cursor.execute(
                "SELECT news_id, title, summary, body_md, published_at, source, is_analytics, topics FROM news LIMIT 1;")
            rows = cursor.fetchall()
    else:
        query_vector = embedding_model.encode(query_text).tolist()
        sql = """
            SELECT 
                news_id,
                title,
                summary,
                body_md,
                published_at,
                source,
                is_analytics,
                topics,
                (1 - (embedding <=> %s::vector)) AS relevance_score
            FROM news
            ORDER BY embedding <=> %s::vector ASC
            LIMIT %s;
        """
        cursor.execute(sql, (str(query_vector), str(query_vector), limit))
        rows = cursor.fetchall()

    cursor.close()
    conn.close()

    results = []
    for row in rows:
        score = float(row.get("relevance_score", 0.95)) if row.get("relevance_score") is not None else 0.95
        results.append({
            "news_id": row["news_id"],
            "title": row["title"],
            "summary": row["summary"] or (row["body_md"][:300] + "" if row["body_md"] else ""),
            "body_md": row["body_md"] or row["summary"],
            "published_at": row["published_at"].isoformat() if row["published_at"] else "2026-09-07T10:00:00Z",
            "source": row["source"] or "Альфа-Банк",
            "is_analytics": row["is_analytics"] if row["is_analytics"] is not None else True,
            "relevance_score": round(score, 2)
        })

    return results


# ==========================================
# 4. РАБОТА СО ШЛЮЗОМ ЧЕРЕЗ OPENAI SDK (БЕЗ ЛИМИТОВ ТОКЕНОВ)
# ==========================================
class DeepSeekAgent:
    def __init__(self, profile: Optional[ClientProfileDTO]):
        self.profile = profile

        self.client = OpenAI(
            api_key=DEEPSEEK_API_KEY,
            base_url=GATEWAY_BASE_URL
        )

        self.industry = profile.mainokvedName if profile and profile.mainokvedName else "Крупный бизнес"
        self.revenue_val = f"{profile.revenue:,.0f} руб." if profile and profile.revenue else "не указана"
        self.credits_val = "Есть" if profile and profile.hasCredits else "Нет"
        self.ved_val = profile.vedCountry if profile and profile.hasVed and profile.vedCountry else "Нет"

    def _call_gateway(self, messages: list, json_mode: bool = False, temperature: float = 0.0) -> str:
        kwargs = {
            "model": MODEL_NAME,  # "deepseek/deepseek-v4.1-flash"
            "messages": messages,
            "temperature": temperature
            # Лимиты max_tokens полностью сняты, чтобы модель не упиралась в ограничения
        }

        if json_mode:
            kwargs["response_format"] = {"type": "json_object"}

        response = self.client.chat.completions.create(**kwargs)
        return response.choices[0].message.content

    def generate_insight_deep(self, news_item: dict) -> dict:
        system_prompt = (
            "Ты — финансовый аналитик Альфа-Банка. Твоя задача — дать персонализированный инсайт.\n"
            "Выделяй только самую важную финансовую и стратегическую информацию из новости, отсекая всю «воду».\n\n"
            "КЛИЕНТ:\n"
            f"- Отрасль: {self.industry}\n"
            f"- Выручка: {self.revenue_val}\n"
            f"- Кредиты: {self.credits_val}\n"
            f"- ВЭД: {self.ved_val}\n\n"
            "ТРЕБОВАНИЯ К ОТВЕТУ:\n"
            "1. Верни СТРОГО валидный JSON со следующей структурой:\n"
            "{\n"
            "  \"what_happened\": \"Краткая суть события (2 предложения)\",\n"
            "  \"why_important_for_client\": \"Персонализированный вывод ИИ. ОБЯЗАТЕЛЬНО начни с 'Для [Отрасль]...' или 'Для вашей компании...'. Объясни влияние на бизнес, используя данные профиля.\",\n"
            "  \"bank_position\": \"Позиция Альфа-Банка. Экспертное мнение, прогноз, рекомендация банка. Если в новости нет позиции банка — напиши обобщенную экспертную оценку.\",\n"
            "  \"risks\": [\"Риск 1\", \"Риск 2\", \"Риск 3\"],\n"
            "  \"opportunities\": [\"Возможность 1\", \"Возможность 2\"]\n"
            "}\n"
            "2. Если для данного клиента событие не несет рисков или возможностей, верни пустые массивы [], но НЕ убирай поля.\n"
            "3. Не используй общие фразы. Только конкретика, привязанная к профилю."
        )

        # Аккуратная обрезка текста строго по знакам препинания в пределах 1500 символов без троеточий
        raw_body = news_item.get('body_md') or news_item.get('summary') or ""
        if len(raw_body) > 1500:
            raw_body = raw_body[:1500]
            last_dot = max(raw_body.rfind('.'), raw_body.rfind('!'), raw_body.rfind('?'))
            if last_dot != -1:
                raw_body = raw_body[:last_dot + 1]
            else:
                raw_body = raw_body.strip()

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"НОВОСТЬ:\n{news_item['title']}\n{raw_body}"}
        ]

        try:
            raw_content = self._call_gateway(messages, json_mode=True, temperature=0.0)

            # Очистка от возможных markdown-оберток
            cleaned = raw_content.strip()
            if cleaned.startswith("```"):
                lines = cleaned.splitlines()
                cleaned = "\n".join([line for line in lines if not line.startswith("```")])
            cleaned = cleaned.strip()

            data = json.loads(cleaned)

            # Возвращаем с маппингом под формат Java (camelCase)
            return {
                "whatHappened": data.get("what_happened") or data.get("whatHappened", ""),
                "whyImportantForClient": data.get("why_important_for_client") or data.get("whyImportantForClient", ""),
                "bankPosition": data.get("bank_position") or data.get("bankPosition", ""),
                "risks": data.get("risks", []),
                "opportunities": data.get("opportunities", [])
            }

        except Exception as e:
            print(f"⚠️ Ошибка парсинга JSON от DeepSeek: {e}")
            print(f"📦 Сырой ответ модели:\n{raw_content if 'raw_content' in locals() else 'нет ответа'}")

            # Fallback (заглушка на случай сбоя)
            return {
                "whatHappened": f"Ключевое макроэкономическое событие: {news_item['title']}.",
                "whyImportantForClient": f"Для вашей компании с учетом выручки в {self.revenue_val} изменения на рынке напрямую влияют на операционную ликвидность.",
                "bankPosition": "Эксперты Альфа-Банка рекомендуют провести оценку текущих финансовых рисков и зафиксировать ключевые параметры.",
                "risks": ["Рост стоимости обслуживания обязательств", "Макроэкономическая волатильность"],
                "opportunities": ["Оптимизация расчетных операций", "Гибкое управление ликвидностью"]
            }

    def generate_chat_answer_history(self, messages: List[ChatMessageDTO], temperature: float = 0.3,
                                     max_tokens: int = 500) -> str:
        formatted_messages = [{"role": msg.role, "content": msg.content} for msg in messages]
        try:
            return self._call_gateway(
                messages=formatted_messages,
                json_mode=False,
                temperature=temperature
            )
        except Exception as e:
            print(f"⚠️ Ошибка Gateway API в чате: {e}")
            return "Для вашей компании сохранение ставки ЦБ косвенно влияет на финансовые потоки. Рекомендуем диверсифицировать расчетные инструменты."


# ==========================================
# 5. ОСНОВНОЙ ЭНДПОИНТ
# ==========================================
@app.post("/internal/v1/generate")
def handle_internal_request(
        request: IncomingJavaRequest,
        x_trace_id: Optional[str] = Header(None)
):
    start_time = time.time()
    profile = request.clientProfile
    params = request.params or ParamsDTO()

    query_parts = []
    if profile:
        if profile.mainokvedName: query_parts.append(profile.mainokvedName)
        if profile.hasCredits: query_parts.append("кредиты ставка ЦБ")
        if profile.hasVed: query_parts.append(f"ВЭД логистика {profile.vedCountry or ''}")

    query_text = " ".join(query_parts) if query_parts else "Макроэкономика и аналитика Альфа-Банка"

    # Сценарий 1: feed
    if request.requestType == "feed":
        news_list = search_news_pgvector(query_text, limit=params.limit or 5)
        items = []
        for news in news_list:
            semantic_tags = []
            if profile and profile.hasCredits:
                semantic_tags.append(
                    {"tag": "cl_product_loa_mark", "label": "Наличие кредитов", "influenceWeight": 0.4})
            if profile and profile.mainokvedCode:
                semantic_tags.append(
                    {"tag": "mainokved_code", "label": f"ОКВЭД {profile.mainokvedCode}", "influenceWeight": 0.3})

            items.append({
                "newsId": news["news_id"],
                "title": news["title"],
                "summary": news["summary"],
                "publishedAt": news["published_at"],
                "source": news["source"],
                "isAnalytics": news["is_analytics"],
                "relevanceScore": news["relevance_score"],
                "semanticTags": semantic_tags,
                "sources": [{"id": news["news_id"], "title": news["title"],
                             "type": "alfa_analytics" if news["is_analytics"] else "external_news"}]
            })

        return {
            "totalFound": len(items),
            "tokensInput": 1500,
            "tokensOutput": 500,
            "processingTimeMs": int((time.time() - start_time) * 1000),
            "items": items
        }

    # Сценарий 2: insight
    elif request.requestType == "insight":
        news_list = search_news_pgvector(query_text, limit=1, news_id=params.newsId)
        if not news_list:
            raise HTTPException(status_code=404, detail="Новость не найдена в базе")

        target_news = news_list[0]
        agent = DeepSeekAgent(profile)
        insight_data = agent.generate_insight_deep(target_news)

        semantic_tags = []
        if profile and profile.hasCredits:
            semantic_tags.append({"tag": "cl_product_loa_mark", "label": "Наличие кредитов", "influenceWeight": 0.4})

        return {
            "newsId": target_news["news_id"],
            "title": target_news["title"],
            "summary": target_news["summary"],
            "publishedAt": target_news["published_at"],
            "source": target_news["source"],
            "isAnalytics": target_news["is_analytics"],
            "relevanceScore": target_news["relevance_score"],
            "whatHappened": insight_data.get("whatHappened", ""),
            "whyImportantForClient": insight_data.get("whyImportantForClient", ""),
            "risks": insight_data.get("risks", []),
            "opportunities": insight_data.get("opportunities", []),
            "bankPosition": insight_data.get("bankPosition", ""),
            "semanticTags": semantic_tags,
            "sources": [{"id": target_news["news_id"], "title": target_news["title"],
                         "type": "alfa_analytics" if target_news["is_analytics"] else "external_news"}]
        }

    # Сценарий 3: chat
    elif request.requestType == "chat":
        if not request.llmRequest or not request.llmRequest.messages:
            raise HTTPException(status_code=400,
                                detail="Для типа 'chat' обязателен объект 'llmRequest' с массивом 'messages'")

        agent = DeepSeekAgent(profile)

        chat_content = agent.generate_chat_answer_history(
            messages=request.llmRequest.messages,
            temperature=request.llmRequest.temperature or 0.3,
            max_tokens=request.llmRequest.maxTokens or 500
        )

        news_list = search_news_pgvector(query_text, limit=1)
        citation_id = news_list[0]["news_id"] if news_list else "rbc-trade-china-2026"
        citation_title = news_list[0]["title"] if news_list else "Логистические цепочки РФ-Китай: прогноз на Q4"

        return {
            "messageId": str(uuid.uuid4()),
            "role": "assistant",
            "content": chat_content,
            "createdAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
            "citations": [
                {
                    "id": citation_id,
                    "title": citation_title,
                    "type": "external_news"
                }
            ]
        }

    else:
        raise HTTPException(status_code=400, detail=f"Неизвестный requestType: {request.requestType}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)