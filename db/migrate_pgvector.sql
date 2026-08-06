-- Idempotent migration for existing databases
-- DashScope text-embedding-v3 supports dims in [64,128,256,512,768,1024]
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop old index / column if dimension mismatches (1536 -> 1024)
DROP INDEX IF EXISTS keyword_embedding_idx;
ALTER TABLE keyword_daily DROP COLUMN IF EXISTS embedding;
ALTER TABLE keyword_daily ADD COLUMN IF NOT EXISTS embedding vector(1024);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'keyword_embedding_idx'
    ) THEN
        BEGIN
            CREATE INDEX keyword_embedding_idx
                ON keyword_daily USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Skip ivfflat index: %', SQLERRM;
        END;
    END IF;
END $$;
