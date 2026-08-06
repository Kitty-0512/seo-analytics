-- SEO Analytics Platform - PostgreSQL DDL
-- Supports upsert via ON CONFLICT on unique indexes

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TYPE platform_type AS ENUM ('GSC', 'BING');

-- Sites
CREATE TABLE site (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    domain          VARCHAR(512) NOT NULL,
    gsc_property    VARCHAR(512),
    bing_site_url   VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Platform OAuth / API credentials
CREATE TABLE platform_auth (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    platform        platform_type NOT NULL,
    access_token    TEXT,
    refresh_token   TEXT,
    api_key         TEXT,
    token_expiry    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (site_id, platform)
);

-- Daily traffic aggregates
CREATE TABLE traffic_daily (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    platform        platform_type NOT NULL,
    stat_date       DATE NOT NULL,
    clicks          BIGINT NOT NULL DEFAULT 0,
    impressions     BIGINT NOT NULL DEFAULT 0,
    ctr             DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_position    DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_traffic_daily
    ON traffic_daily (site_id, platform, stat_date);

-- pgvector for semantic keyword search (extension created at top)

-- Daily keyword metrics
CREATE TABLE keyword_daily (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    platform        platform_type NOT NULL,
    stat_date       DATE NOT NULL,
    keyword         VARCHAR(1024) NOT NULL,
    clicks          BIGINT NOT NULL DEFAULT 0,
    impressions     BIGINT NOT NULL DEFAULT 0,
    avg_position    DOUBLE PRECISION NOT NULL DEFAULT 0,
    embedding       vector(1024)
);

CREATE UNIQUE INDEX uk_keyword_daily
    ON keyword_daily (site_id, platform, stat_date, keyword);

-- IVFFlat may fail on empty tables; ignore and create later via migrate_pgvector.sql
DO $$
BEGIN
    CREATE INDEX keyword_embedding_idx
        ON keyword_daily USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Skip ivfflat index on init: %', SQLERRM;
END $$;

-- Daily page metrics
CREATE TABLE page_daily (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    platform        platform_type NOT NULL,
    stat_date       DATE NOT NULL,
    page_url        VARCHAR(2048) NOT NULL,
    clicks          BIGINT NOT NULL DEFAULT 0,
    impressions     BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_page_daily
    ON page_daily (site_id, platform, stat_date, page_url);

-- Keyword ↔ page daily mapping (GSC date+query+page)
CREATE TABLE keyword_page_daily (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    platform        platform_type NOT NULL,
    stat_date       DATE NOT NULL,
    keyword         VARCHAR(1024) NOT NULL,
    page_url        VARCHAR(2048) NOT NULL,
    clicks          BIGINT NOT NULL DEFAULT 0,
    impressions     BIGINT NOT NULL DEFAULT 0,
    avg_position    DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_keyword_page_daily
    ON keyword_page_daily (site_id, platform, stat_date, keyword, page_url);

-- AI chat history
CREATE TABLE ai_chat (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT REFERENCES site(id) ON DELETE SET NULL,
    question        TEXT NOT NULL,
    sql_generated   TEXT,
    data_json       TEXT,
    answer          TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Helpful indexes for dashboard queries
CREATE INDEX idx_traffic_daily_site_date ON traffic_daily (site_id, stat_date);
CREATE INDEX idx_keyword_daily_site_date ON keyword_daily (site_id, stat_date);
CREATE INDEX idx_page_daily_site_date ON page_daily (site_id, stat_date);
CREATE INDEX idx_keyword_page_daily_site_date ON keyword_page_daily (site_id, stat_date);
CREATE INDEX idx_ai_chat_site ON ai_chat (site_id);

-- Upsert helper comments (used by MyBatis-Plus / custom SQL):
-- INSERT INTO traffic_daily (...) VALUES (...)
--   ON CONFLICT (site_id, platform, stat_date)
--   DO UPDATE SET clicks=EXCLUDED.clicks, impressions=EXCLUDED.impressions,
--                 ctr=EXCLUDED.ctr, avg_position=EXCLUDED.avg_position;
--
-- INSERT INTO keyword_daily (...) VALUES (...)
--   ON CONFLICT (site_id, platform, stat_date, keyword)
--   DO UPDATE SET clicks=EXCLUDED.clicks, impressions=EXCLUDED.impressions,
--                 avg_position=EXCLUDED.avg_position;
--
-- INSERT INTO page_daily (...) VALUES (...)
--   ON CONFLICT (site_id, platform, stat_date, page_url)
--   DO UPDATE SET clicks=EXCLUDED.clicks, impressions=EXCLUDED.impressions;
