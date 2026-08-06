-- Idempotent bootstrap for Render Postgres (run once in Dashboard ? Shell / psql)
CREATE EXTENSION IF NOT EXISTS vector;

DO $$ BEGIN
    CREATE TYPE platform_type AS ENUM ('GSC', 'BING');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS site (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    domain          VARCHAR(512) NOT NULL,
    gsc_property    VARCHAR(512),
    bing_site_url   VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS platform_auth (
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

CREATE TABLE IF NOT EXISTS traffic_daily (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    platform        platform_type NOT NULL,
    stat_date       DATE NOT NULL,
    clicks          BIGINT NOT NULL DEFAULT 0,
    impressions     BIGINT NOT NULL DEFAULT 0,
    ctr             DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_position    DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_traffic_daily
    ON traffic_daily (site_id, platform, stat_date);

CREATE TABLE IF NOT EXISTS keyword_daily (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_keyword_daily
    ON keyword_daily (site_id, platform, stat_date, keyword);

DO $$
BEGIN
    CREATE INDEX IF NOT EXISTS keyword_embedding_idx
        ON keyword_daily USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Skip ivfflat index: %', SQLERRM;
END $$;

CREATE TABLE IF NOT EXISTS page_daily (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    platform        platform_type NOT NULL,
    stat_date       DATE NOT NULL,
    page_url        VARCHAR(2048) NOT NULL,
    clicks          BIGINT NOT NULL DEFAULT 0,
    impressions     BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_page_daily
    ON page_daily (site_id, platform, stat_date, page_url);

CREATE TABLE IF NOT EXISTS keyword_page_daily (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_keyword_page_daily
    ON keyword_page_daily (site_id, platform, stat_date, keyword, page_url);

CREATE TABLE IF NOT EXISTS ai_chat (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT REFERENCES site(id) ON DELETE SET NULL,
    question        TEXT NOT NULL,
    sql_generated   TEXT,
    data_json       TEXT,
    answer          TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_traffic_daily_site_date ON traffic_daily (site_id, stat_date);
CREATE INDEX IF NOT EXISTS idx_keyword_daily_site_date ON keyword_daily (site_id, stat_date);
CREATE INDEX IF NOT EXISTS idx_page_daily_site_date ON page_daily (site_id, stat_date);
CREATE INDEX IF NOT EXISTS idx_keyword_page_daily_site_date ON keyword_page_daily (site_id, stat_date);
CREATE INDEX IF NOT EXISTS idx_ai_chat_site ON ai_chat (site_id);
