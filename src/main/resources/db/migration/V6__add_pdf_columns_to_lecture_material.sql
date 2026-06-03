-- lecture_material 테이블에 PDF 뷰어 컬럼 추가
ALTER TABLE lecture_material
    ADD COLUMN IF NOT EXISTS file_url TEXT,
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(512),
    ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS saved_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS page_count INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS allow_download BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS uploaded_by BIGINT REFERENCES users(id);

-- 읽기 진행도 테이블 생성
CREATE TABLE IF NOT EXISTS user_material_progress (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT    NOT NULL,
    material_id  BIGINT    NOT NULL REFERENCES lecture_material(id) ON DELETE CASCADE,
    current_page INT       NOT NULL,
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_material_progress UNIQUE (user_id, material_id)
);

CREATE INDEX IF NOT EXISTS idx_ump_user_id     ON user_material_progress (user_id);
CREATE INDEX IF NOT EXISTS idx_ump_material_id ON user_material_progress (material_id);