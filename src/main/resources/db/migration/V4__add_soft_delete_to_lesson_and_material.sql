-- 2026-05-26 — lesson, lecture_material 소프트 삭제 컬럼 추가
--
-- 배경:
--   강의(lesson)·교안(lecture_material) 삭제 시 하위 FK 참조(lecture_material, quiz)로
--   인해 하드 삭제가 불가능했다. Quiz 에 이미 적용된 @SQLDelete + @SQLRestriction 패턴을
--   동일하게 적용하기 위해 deleted, deleted_at 컬럼을 추가한다.
--
-- 적용 후 동작:
--   JPA delete() 호출 → UPDATE ... SET deleted = true, deleted_at = NOW() WHERE id = ?
--   모든 SELECT 쿼리에 WHERE deleted = false 가 자동 추가됨 (@SQLRestriction)
--
-- 멱등성: ADD COLUMN IF NOT EXISTS 사용

ALTER TABLE lesson
    ADD COLUMN IF NOT EXISTS deleted    BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE lecture_material
    ADD COLUMN IF NOT EXISTS deleted    BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
