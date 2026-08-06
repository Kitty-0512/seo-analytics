# Deploy to Render (public URL)

本项目用 Render **Blueprint**（`render.yaml`）部署，会得到公网地址，例如：

- 前端：`https://seo-web-xxxx.onrender.com`
- API：`https://seo-api-xxxx.onrender.com`
- AI：`https://seo-ai-xxxx.onrender.com`（前端不直接访问，API 内网调用）

## 前置条件

1. [Render](https://render.com) 账号
2. GitHub 仓库（本目录需 `git push` 上去）
3. 准备好环境变量：`GSC_*`、`LLM_*`、`BING_API_KEY`（可选）

> Free 实例会休眠；首次打开可能要等 30–60 秒唤醒。Postgres Free 有使用期限，适合作品集演示。

## 步骤

### 1. 推送到 GitHub

```bash
cd seo-analytics
git init
git add .
git commit -m "chore: add Render Blueprint for public deploy"
# 在 GitHub 新建空仓库后：
git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git branch -M main
git push -u origin main
```

### 2. 用 Blueprint 一键创建

1. 打开 [Render Dashboard](https://dashboard.render.com) → **New** → **Blueprint**
2. 连接上述 GitHub 仓库，确认 Blueprint 文件为 `render.yaml`
3. Apply 时按提示填写（`sync: false` 的项）：

| 变量 | 填什么 |
|------|--------|
| `LLM_BASE_URL` | 如 `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| `LLM_API_KEY` | 你的 Key |
| `GSC_CLIENT_ID` / `GSC_CLIENT_SECRET` | Google OAuth |
| `GSC_REDIRECT_URI` | 先填占位，部署完 API 后再改成 `https://seo-api-xxxx.onrender.com/api/auth/gsc/callback` |
| `FRONTEND_URL` | 先占位，部署完前端后再改成 `https://seo-web-xxxx.onrender.com` |
| `VITE_API_BASE_URL` | 先占位，部署完 API 后再改成 `https://seo-api-xxxx.onrender.com/api` |

### 3. 初始化数据库

Render Postgres **不会**自动跑 `db/init.sql`。部署成功后：

1. Dashboard → **seo-postgres** → **Connect** / **PSQL**
2. 粘贴执行 [`db/render_bootstrap.sql`](../db/render_bootstrap.sql) 全文
3. 确认：`CREATE EXTENSION vector;` 成功（RAG/embed 需要）

### 4. 回填公网 URL（重要）

部署完成后，在 Dashboard 复制真实域名，更新环境变量并 **Manual Deploy**：

**seo-api**

- `FRONTEND_URL` = `https://seo-web-xxxx.onrender.com`
- `GSC_REDIRECT_URI` = `https://seo-api-xxxx.onrender.com/api/auth/gsc/callback`

**seo-web**

- `VITE_API_BASE_URL` = `https://seo-api-xxxx.onrender.com/api`  
  （静态站构建期注入，改完后必须重新 Deploy）

**Google Cloud Console**

- OAuth 已授权重定向 URI 增加同上的 `GSC_REDIRECT_URI`

### 5. 验证

- 打开前端公网 URL
- `https://seo-api-xxxx.onrender.com/api/health` 应返回健康状态
- Settings 里配置站点并 Manual sync

## 架构说明

```text
Browser → seo-web (Static)
            └─ API calls → seo-api (Docker / Spring)
                              ├─ seo-postgres (+ pgvector)
                              ├─ seo-redis
                              └─ seo-ai (Docker / FastAPI, private hostport)
```

## 常见问题

| 问题 | 处理 |
|------|------|
| 前端能开但接口失败 | 检查 `VITE_API_BASE_URL` 是否带 `/api`，并已重新 build |
| API 起不来 / OOM | Free 512MB 偏紧；确认 `JAVA_OPTS=-Xms128m -Xmx384m` |
| embed / 语义搜索失败 | 未执行 bootstrap，或未开 `vector` 扩展 |
| GSC 授权跳错站 | `GSC_REDIRECT_URI` 与 Google Console、`FRONTEND_URL` 不一致 |
| 一直 Loading | Free 休眠，等冷启动；或看 seo-api Logs |

## 费用提示

Blueprint 默认使用 **free** plan。若需常驻不休眠、更大内存，把 `render.yaml` 里对应服务的 `plan` 改为 `starter` 等付费档。
