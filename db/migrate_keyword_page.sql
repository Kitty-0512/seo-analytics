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

CREATE INDEX IF NOT EXISTS idx_keyword_page_daily_site_date
    ON keyword_page_daily (site_id, stat_date);
