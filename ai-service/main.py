"""
SEO Analytics AI Service – Text-to-SQL + RAG via OpenAI-compatible LLM.
Compatible with Qwen (DashScope), Ollama, OpenAI, etc.
"""

from __future__ import annotations

import json
import os
import re
from typing import Any

import psycopg2
import psycopg2.extras
from fastapi import FastAPI, HTTPException
from openai import OpenAI
from pydantic import BaseModel, Field

app = FastAPI(title="SEO Analytics AI Service", version="1.1.0")

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://seo:seo123456@localhost:5432/seo_analytics",
)
LLM_BASE_URL = os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
LLM_API_KEY = os.getenv("LLM_API_KEY", "sk-placeholder")
LLM_MODEL = os.getenv("LLM_MODEL", "qwen-plus")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-v3")
EMBEDDING_DIM = 1024

SCHEMA_SQL = """
Tables (PostgreSQL):

1. site(id BIGINT PK, name VARCHAR, domain VARCHAR, gsc_property VARCHAR, bing_site_url VARCHAR, created_at TIMESTAMP)

2. platform_auth(id BIGINT PK, site_id BIGINT FK→site, platform ENUM('GSC','BING'), access_token TEXT, refresh_token TEXT, api_key TEXT, token_expiry TIMESTAMP, created_at TIMESTAMP)

3. traffic_daily(id BIGINT PK, site_id BIGINT FK→site, platform ENUM('GSC','BING'), stat_date DATE, clicks BIGINT, impressions BIGINT, ctr DOUBLE, avg_position DOUBLE)
   UNIQUE(site_id, platform, stat_date)

4. keyword_daily(id BIGINT PK, site_id BIGINT FK→site, platform ENUM('GSC','BING'), stat_date DATE, keyword VARCHAR, clicks BIGINT, impressions BIGINT, avg_position DOUBLE, embedding vector(1024))
   UNIQUE(site_id, platform, stat_date, keyword)

5. page_daily(id BIGINT PK, site_id BIGINT FK→site, platform ENUM('GSC','BING'), stat_date DATE, page_url VARCHAR, clicks BIGINT, impressions BIGINT)
   UNIQUE(site_id, platform, stat_date, page_url)

6. keyword_page_daily(id BIGINT PK, site_id BIGINT FK→site, platform ENUM('GSC','BING'), stat_date DATE, keyword VARCHAR, page_url VARCHAR, clicks BIGINT, impressions BIGINT, avg_position DOUBLE)
   UNIQUE(site_id, platform, stat_date, keyword, page_url)

7. ai_chat(id BIGINT PK, site_id BIGINT, question TEXT, sql_generated TEXT, data_json TEXT, answer TEXT, created_at TIMESTAMP)

Notes:
- Always filter by site_id when provided.
- Use platform::text = 'GSC' or 'BING' when filtering platform.
- Prefer aggregate queries (SUM/AVG/GROUP BY) for analytics questions.
- Only generate SELECT statements. Never INSERT/UPDATE/DELETE/DROP.
- Do not select the embedding column unless explicitly asked.
- keyword_page_daily maps which landing page ranked for a query (mainly GSC).
"""


class ChatRequest(BaseModel):
    question: str = Field(..., min_length=1)
    site_id: int | None = None


class ChatResponse(BaseModel):
    sql: str
    data: list[dict[str, Any]]
    rag_context: list[dict[str, Any]] = Field(default_factory=list)
    answer: str


class EmbedRequest(BaseModel):
    keywords: list[str] = Field(..., min_length=1)
    site_id: int | None = None


class SemanticSearchRequest(BaseModel):
    query: str = Field(..., min_length=1)
    site_id: int | None = None
    top_k: int = Field(default=5, ge=1, le=50)


def get_llm_client() -> OpenAI:
    return OpenAI(
        api_key=os.environ.get("LLM_API_KEY") or LLM_API_KEY,
        base_url=os.environ.get("LLM_BASE_URL") or LLM_BASE_URL,
    )


def get_connection():
    return psycopg2.connect(DATABASE_URL)


def vector_literal(vec: list[float]) -> str:
    return "[" + ",".join(f"{float(x):.8f}" for x in vec) + "]"


def create_embeddings(texts: list[str]) -> list[list[float]]:
    """Call DashScope text-embedding-v3 and return 1536-d vectors."""
    if not texts:
        return []
    client = get_llm_client()
    # DashScope compatible API supports dimensions for text-embedding-v3
    resp = client.embeddings.create(
        model=EMBEDDING_MODEL,
        input=texts,
        dimensions=EMBEDDING_DIM,
    )
    # Ensure order by index
    sorted_data = sorted(resp.data, key=lambda d: d.index)
    vectors = [list(item.embedding) for item in sorted_data]
    for v in vectors:
        if len(v) != EMBEDDING_DIM:
            raise RuntimeError(f"Expected embedding dim {EMBEDDING_DIM}, got {len(v)}")
    return vectors


def fetch_schema_hint() -> str:
    """Read live column metadata from information_schema as extra context."""
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT table_name, column_name, data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name IN (
                          'site', 'platform_auth', 'traffic_daily',
                          'keyword_daily', 'page_daily', 'keyword_page_daily', 'ai_chat'
                      )
                    ORDER BY table_name, ordinal_position
                    """
                )
                rows = cur.fetchall()
        lines = [f"- {t}.{c} ({d})" for t, c, d in rows]
        return SCHEMA_SQL + "\n\nLive columns:\n" + "\n".join(lines)
    except Exception:
        return SCHEMA_SQL


def llm_chat(system: str, user: str) -> str:
    client = get_llm_client()
    resp = client.chat.completions.create(
        model=LLM_MODEL,
        messages=[
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        temperature=0.1,
    )
    return (resp.choices[0].message.content or "").strip()


def extract_sql(text: str) -> str:
    """Pull SQL out of markdown fences or raw text."""
    fence = re.search(r"```(?:sql)?\s*([\s\S]*?)```", text, re.IGNORECASE)
    if fence:
        sql = fence.group(1).strip()
    else:
        sql = text.strip()
    sql = sql.rstrip(";").strip()
    return sql


def validate_select_only(sql: str) -> None:
    normalized = re.sub(r"\s+", " ", sql.lower()).strip()
    if not normalized.startswith("select") and not normalized.startswith("with"):
        raise HTTPException(status_code=400, detail="Only SELECT/WITH queries are allowed")
    forbidden = ["insert ", "update ", "delete ", "drop ", "alter ", "truncate ", "create ", "grant "]
    for word in forbidden:
        if word in normalized:
            raise HTTPException(status_code=400, detail=f"Forbidden SQL keyword detected: {word.strip()}")


def semantic_search_keywords(
    query: str,
    site_id: int | None = None,
    top_k: int = 5,
) -> list[dict[str, Any]]:
    """pgvector cosine similarity search over keyword_daily.embedding."""
    try:
        query_vec = create_embeddings([query])[0]
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Embedding failed: {e}") from e

    vec = vector_literal(query_vec)
    sql = """
        SELECT
            keyword,
            platform::text AS platform,
            stat_date,
            clicks,
            impressions,
            avg_position,
            site_id,
            1 - (embedding <=> %s::vector) AS score
        FROM keyword_daily
        WHERE embedding IS NOT NULL
          AND (%s::bigint IS NULL OR site_id = %s)
        ORDER BY embedding <=> %s::vector
        LIMIT %s
    """
    with get_connection() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, (vec, site_id, site_id, vec, top_k))
            rows = cur.fetchall()

    result: list[dict[str, Any]] = []
    for row in rows:
        item: dict[str, Any] = {}
        for k, v in dict(row).items():
            if hasattr(v, "isoformat"):
                item[k] = v.isoformat()
            else:
                item[k] = float(v) if k == "score" and v is not None else v
        result.append(item)
    return result


def format_rag_context(rows: list[dict[str, Any]]) -> str:
    if not rows:
        return "（暂无语义检索结果；可能尚未生成关键词 embedding）"
    lines = []
    for i, r in enumerate(rows, 1):
        lines.append(
            f"{i}. keyword={r.get('keyword')}, platform={r.get('platform')}, "
            f"date={r.get('stat_date')}, clicks={r.get('clicks')}, "
            f"impressions={r.get('impressions')}, avg_position={r.get('avg_position')}, "
            f"score={r.get('score')}"
        )
    return "\n".join(lines)


def generate_sql(question: str, site_id: int | None, rag_context: list[dict[str, Any]]) -> str:
    schema = fetch_schema_hint()
    site_hint = f"The current site_id is {site_id}. Always filter by site_id = {site_id}." if site_id else ""
    rag_hint = format_rag_context(rag_context)
    system = (
        "You are a PostgreSQL Text-to-SQL expert for an SEO analytics database. "
        "Use the semantic search context to focus on relevant keywords/pages. "
        "Return ONLY a single SQL SELECT query. No explanation."
    )
    user = (
        f"{schema}\n\n{site_hint}\n\n"
        f"Semantic search context (Top related keywords):\n{rag_hint}\n\n"
        f"User question: {question}"
    )
    raw = llm_chat(system, user)
    sql = extract_sql(raw)
    validate_select_only(sql)
    return sql


def execute_sql(sql: str) -> list[dict[str, Any]]:
    with get_connection() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql)
            rows = cur.fetchall()
            result = []
            for row in rows:
                item = {}
                for k, v in dict(row).items():
                    if hasattr(v, "isoformat"):
                        item[k] = v.isoformat()
                    elif isinstance(v, (bytes, memoryview)):
                        item[k] = str(v)
                    else:
                        item[k] = v
                result.append(item)
            return result


def generate_answer(
    question: str,
    sql: str,
    data: list[dict[str, Any]],
    rag_context: list[dict[str, Any]],
) -> str:
    sample = data[:50]
    system = (
        "你是一名 SEO 数据分析师。根据用户问题、语义检索上下文和 SQL 查询结果，"
        "用简洁清晰的中文给出分析结论。包含关键数字、趋势判断和建议。不要复述 SQL。"
    )
    user = (
        f"用户问题：{question}\n\n"
        f"语义检索上下文：\n{format_rag_context(rag_context)}\n\n"
        f"执行的 SQL：{sql}\n\n"
        f"查询结果（最多 50 行）：{json.dumps(sample, ensure_ascii=False, default=str)}"
    )
    return llm_chat(system, user)


@app.get("/health")
def health():
    return {"status": "ok", "embedding_model": EMBEDDING_MODEL, "llm_model": LLM_MODEL}


@app.post("/embed")
def embed_keywords(req: EmbedRequest):
    """Batch-embed keywords and store into keyword_daily.embedding."""
    keywords = [k.strip() for k in req.keywords if k and k.strip()]
    if not keywords:
        raise HTTPException(status_code=400, detail="keywords is required")

    # Deduplicate while preserving order
    seen: set[str] = set()
    unique_keywords: list[str] = []
    for k in keywords:
        if k not in seen:
            seen.add(k)
            unique_keywords.append(k)

    try:
        vectors = create_embeddings(unique_keywords)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Embedding generation failed: {e}") from e

    updated = 0
    with get_connection() as conn:
        with conn.cursor() as cur:
            for keyword, vec in zip(unique_keywords, vectors):
                lit = vector_literal(vec)
                if req.site_id is not None:
                    cur.execute(
                        """
                        UPDATE keyword_daily
                        SET embedding = %s::vector
                        WHERE keyword = %s AND site_id = %s
                        """,
                        (lit, keyword, req.site_id),
                    )
                else:
                    cur.execute(
                        """
                        UPDATE keyword_daily
                        SET embedding = %s::vector
                        WHERE keyword = %s
                        """,
                        (lit, keyword),
                    )
                updated += cur.rowcount
        conn.commit()

    return {
        "embedded": len(unique_keywords),
        "rows_updated": updated,
        "dimensions": EMBEDDING_DIM,
        "model": EMBEDDING_MODEL,
    }


@app.post("/search/semantic")
def search_semantic(req: SemanticSearchRequest):
    rows = semantic_search_keywords(req.query, req.site_id, req.top_k)
    return {"query": req.query, "results": rows}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    # RAG: question -> embedding -> pgvector Top-5
    try:
        rag_context = semantic_search_keywords(req.question, req.site_id, top_k=5)
    except HTTPException as e:
        # If embedding fails (e.g. missing API key), continue without RAG
        if e.status_code == 502:
            rag_context = []
        else:
            raise
    except Exception:
        rag_context = []

    try:
        sql = generate_sql(req.question, req.site_id, rag_context)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"LLM SQL generation failed: {e}") from e

    try:
        data = execute_sql(sql)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"SQL execution failed: {e}; sql={sql}") from e

    try:
        answer = generate_answer(req.question, sql, data, rag_context)
    except Exception as e:
        answer = f"数据已查出（{len(data)} 行），但生成分析失败：{e}"

    return ChatResponse(sql=sql, data=data, rag_context=rag_context, answer=answer)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=5001)
