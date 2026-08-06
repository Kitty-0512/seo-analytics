# SEO Analytics Platform

自托管 SEO 数据分析平台：同步 Google Search Console / Bing Webmaster 数据，可视化 KPI 与趋势，并提供 Query–Page 映射、机会洞察（Low CTR / Striking distance）及 Text-to-SQL AI 问答。

## Live Demo

| 服务 | 地址 |
|------|------|
| **前端（作品集入口）** | [https://seo-web-yih1.onrender.com](https://seo-web-yih1.onrender.com) |
| **API 健康检查** | [https://seo-api-dsgo.onrender.com/api/health](https://seo-api-dsgo.onrender.com/api/health) |
| **源码** | [github.com/Kitty-0512/seo-analytics](https://github.com/Kitty-0512/seo-analytics) |

> 部署在 [Render](https://render.com) Free 实例上，冷启动可能需要 30–60 秒。首次访问若加载较慢，稍等后刷新即可。

### 演示站点配置示例

在 Settings → Add site 时可参考（对接 [Kitty Blog](https://Kitty-0512.github.io)）：

| 字段 | 值 |
|------|-----|
| Name | `Kitty Blog` |
| Domain | `https://Kitty-0512.github.io` |
| GSC property | `https://Kitty-0512.github.io/` |
| Bing site URL | `https://Kitty-0512.github.io/` |

GSC OAuth 与 Bing API Key 需在 Render 环境变量中自行配置；未配置时仍可浏览 UI，同步与 AI 功能需凭证后可用。

---

## Features

- **多站点管理**：GSC OAuth2 + Bing API Key
- **定时 / 手动同步**：每日 cron 拉取最近 N 天；traffic / keyword / page **按天入库**
- **Query–Page 映射**：GSC `date+query+page` 三元组 → `/seo/query-page`
- **机会洞察**：Striking distance、Low CTR → `/seo/opportunities`
- **Dashboard**：KPI、趋势图、Top 关键词
- **平台对比**：GSC vs Bing 周期 / 页面 / 平台视图
- **AI 问答**：自然语言 → SQL → 结果 + 中文解读（通义千问 / OpenAI 兼容 API）
- **语义搜索**：关键词 sync 后自动 embed（pgvector + DashScope embedding）
- **双部署方式**：本地 Docker Compose；公网 Render Blueprint

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3, MyBatis-Plus, Java 17 |
| Frontend | Vue 3, Element Plus, ECharts, Pinia, Vite |
| Database | PostgreSQL 15 + pgvector |
| Cache | Redis / Valkey |
| AI | Python FastAPI, OpenAI-compatible LLM |
| Deploy | Docker Compose + Nginx；[Render Blueprint](render.yaml) |

---

## Architecture

### 公网（Render）

```
Browser → seo-web (Static CDN)
            └─ HTTPS → seo-api (Spring Boot)
                          ├─ seo-postgres (+ pgvector)
                          ├─ seo-redis (Valkey)
                          └─ seo-ai (FastAPI, 内网)
                                    ↓
                          GSC / Bing / DashScope API
```

### 本地（Docker Compose）

```
Browser :80 → Nginx → analytics-web (静态) + analytics-api :8080
                              ├─ postgres:5432
                              ├─ redis:6379
                              └─ ai-service:5001
```

---

## Project Structure

```
seo-analytics/
├── render.yaml              # Render Blueprint（公网一键部署）
├── docker-compose.yml       # 本地全栈
├── db/
│   ├── init.sql
│   └── render_bootstrap.sql # Render Postgres 初始化
├── docs/DEPLOY_RENDER.md    # Render 部署详细步骤
├── analytics-api/           # Spring Boot
├── analytics-web/           # Vue 3
└── ai-service/              # FastAPI
```

---

## Quick Start（本地）

### 1. 环境变量

```bash
cp .env.example .env
# 编辑 .env：LLM_API_KEY、GSC_*、BING_API_KEY 等（勿提交 .env）
```

### 2. 构建前端并启动

```bash
cd analytics-web && npm install && npm run build && cd ..
docker compose up -d --build
```

打开 http://localhost

### 3. 开发模式（可选）

```bash
docker compose up -d postgres redis
# 终端 1: cd analytics-api && mvn spring-boot:run
# 终端 2: cd ai-service && pip install -r requirements.txt && uvicorn main:app --port 5001
# 终端 3: cd analytics-web && npm run dev   → http://localhost:5173
```

---

## Deploy to Render

完整步骤见 **[docs/DEPLOY_RENDER.md](docs/DEPLOY_RENDER.md)**。

概要：

1. Fork / clone 本仓库，推送到 GitHub
2. Render → **New → Blueprint** → 选择仓库（`render.yaml`）
3. 填写 `LLM_*`、`GSC_*` 等环境变量
4. 在 **seo-postgres** 执行 `db/render_bootstrap.sql`
5. 设置 `VITE_API_BASE_URL`、`FRONTEND_URL`、`GSC_REDIRECT_URI` 后 redeploy **seo-web** / **seo-api**

---

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | 健康检查 |
| GET/POST | `/api/sites` | 站点 CRUD |
| GET | `/api/auth/gsc/authorize` | GSC OAuth 跳转 |
| POST | `/api/sync` | 手动同步 |
| GET | `/api/sync/status` | 最近同步时间 |
| GET | `/api/dashboard` | KPI + 趋势 |
| GET | `/api/seo` | 关键词 / 页面 |
| GET | `/api/seo/query-page` | Query–Page 映射 |
| GET | `/api/seo/opportunities` | 机会洞察 |
| POST | `/api/ai/chat` | AI Text-to-SQL |

---

## Usage Flow

1. **Settings** → Add site → Connect GSC / Save Bing Key
2. **Manual sync** 拉取最近 7 天数据
3. **Dashboard** / **SEO**（Keywords · Pages · Query–Page · Opportunities）
4. **AI Chat** 用自然语言问 SEO 数据

---

## Environment Variables

见 [.env.example](.env.example)。**切勿将 `.env` 提交到 Git**（已在 `.gitignore`）。

| Variable | Description |
|----------|-------------|
| `GSC_CLIENT_ID` / `GSC_CLIENT_SECRET` | Google OAuth |
| `GSC_REDIRECT_URI` | 生产环境指向 `https://<seo-api>/api/auth/gsc/callback` |
| `FRONTEND_URL` | 生产环境前端 URL（OAuth 回跳） |
| `VITE_API_BASE_URL` | 前端构建时 API 根路径（Render **seo-web**） |
| `BING_API_KEY` | Bing Webmaster API |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | 通义千问或 OpenAI 兼容端点 |

---

## License

MIT
