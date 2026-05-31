-- PDF 뷰어 메타데이터 컬럼 및 읽기 진행도 테이블
ALTER TABLE lesson_pdfs
    ADD COLUMN IF NOT EXISTS title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS page_count INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS aspect_ratio VARCHAR(32) DEFAULT '0.707',
    ADD COLUMN IF NOT EXISTS thumbnail_url TEXT,
    ADD COLUMN IF NOT EXISTS allow_download BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE lesson_pdfs
SET title = original_file_name
WHERE title IS NULL;

ALTER TABLE lesson_pdfs
    ALTER COLUMN title SET NOT NULL;

CREATE TABLE IF NOT EXISTS user_material_progress (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    material_id BIGINT       NOT NULL REFERENCES lesson_pdfs (id) ON DELETE CASCADE,
    current_page INT         NOT NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_material_progress UNIQUE (user_id, material_id)
);

CREATE INDEX IF NOT EXISTS idx_user_material_progress_user_id ON user_material_progress (user_id);
CREATE INDEX IF NOT EXISTS idx_user_material_progress_material_id ON user_material_progress (material_id);
