# SEO Analytics Platform

Personal SEO data analytics platform that syncs Google Search Console and Bing Webmaster metrics, visualizes KPIs/trends, and answers questions via Text-to-SQL AI.

## Public deploy (Render)

要获取公网访问网址，见 **[docs/DEPLOY_RENDER.md](docs/DEPLOY_RENDER.md)**（Blueprint：`render.yaml`）。

## Features

- Multi-site management with GSC OAuth2 and Bing API Key authorization
- Manual sync of daily traffic, keywords, and pages (upsert into PostgreSQL)
- Dashboard with KPI cards, traffic trend charts, and Top 10 keywords
- SEO data tables with GSC / Bing platform switching
- AI chat: natural language → SQL → Chinese analysis (OpenAI-compatible LLM)
- Docker Compose one-command deployment behind Nginx

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3, MyBatis-Plus, Java 17 |
| Frontend | Vue 3, Element Plus, ECharts, Pinia, Vite |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| AI Service | Python FastAPI + OpenAI SDK (Text-to-SQL) |
| Deploy | Docker Compose + Nginx；公网可用 [Render Blueprint](docs/DEPLOY_RENDER.md) |

## Architecture

```
                    ┌─────────────┐
                    │   Browser   │
                    └──────┬──────┘
                           │ :80
                    ┌──────▼──────┐
                    │    Nginx    │
                    └──┬───────┬──┘
              /        │       │ /api/
       static files    │       │
              │        │       ▼
   ┌──────────▼──┐   │  ┌─────────────────┐
   │ analytics-  │   │  │  analytics-api  │
   │ web (Vue3)  │   │  │  (Spring Boot)  │
   └─────────────┘   │  └──┬─────┬────┬───┘
                     │     │     │    │
                     │     │     │    └──► ai-service:5001 (FastAPI)
                     │     │     │              │
                     │     ▼     ▼              ▼
                     │  postgres:5432      (reads schema
                     │  redis:6379          & executes SQL)
                     │
                     └── GSC / Bing APIs (outbound sync)
```

## Project Structure

```
seo-analytics/
├── docker-compose.yml
├── .env.example
├── db/init.sql
├── nginx/nginx.conf
├── analytics-api/          # Spring Boot backend
├── analytics-web/          # Vue3 frontend
├── ai-service/             # FastAPI Text-to-SQL
└── README.md
```

## Quick Start

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env – at least set LLM_API_KEY for AI chat
# Optionally set GSC_CLIENT_ID / GSC_CLIENT_SECRET / BING_API_KEY
```

### 2. Build frontend

```bash
cd analytics-web
npm install
npm run build
cd ..
```

### 3. Start all services

```bash
docker compose up -d --build
```

Open http://localhost

### Local development (without Docker for apps)

```bash
# Start only infra
docker compose up -d postgres redis

# Backend
cd analytics-api
mvn spring-boot:run

# AI service
cd ai-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 5001 --reload

# Frontend
cd analytics-web
npm run dev
```

Frontend dev server: http://localhost:5173 (proxies `/api` → `:8080`)

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | `seo_analytics` |
| `POSTGRES_USER` | DB user | `seo` |
| `POSTGRES_PASSWORD` | DB password | `seo123456` |
| `REDIS_PASSWORD` | Redis password | `redis123456` |
| `GSC_CLIENT_ID` | Google OAuth client ID | — |
| `GSC_CLIENT_SECRET` | Google OAuth client secret | — |
| `GSC_REDIRECT_URI` | OAuth callback URL | `http://localhost/api/auth/gsc/callback` |
| `BING_API_KEY` | Optional default Bing key | — |
| `LLM_BASE_URL` | OpenAI-compatible base URL | DashScope compatible endpoint |
| `LLM_API_KEY` | LLM API key | — |
| `LLM_MODEL` | Model name | `qwen-plus` |
| `AI_SERVICE_URL` | AI service URL (backend) | `http://ai-service:5001` |

### LLM examples

**Qwen (DashScope):**
```env
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_API_KEY=sk-xxxx
LLM_MODEL=qwen-plus
```

**Local Ollama:**
```env
LLM_BASE_URL=http://host.docker.internal:11434/v1
LLM_API_KEY=ollama
LLM_MODEL=llama3
```

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/api/sites` | List / create sites |
| GET | `/api/auth/gsc/authorize` | Get GSC OAuth URL |
| POST | `/api/auth/bing` | Save Bing API key |
| POST | `/api/sync` | Manual sync (`siteId`, optional `platform`) |
| GET | `/api/dashboard` | KPI + trend + top keywords |
| GET | `/api/seo` | Keywords & pages tables |
| POST | `/api/ai/chat` | AI Text-to-SQL chat |

## Usage Flow

1. Open **Settings** → add a site (domain / GSC property / Bing URL)
2. Connect **GSC** (OAuth) and/or save **Bing API Key**
3. Click **Sync Now** to pull search analytics
4. View **Dashboard** and **SEO Data**
5. Ask questions on **AI Chat**

## License

MIT
