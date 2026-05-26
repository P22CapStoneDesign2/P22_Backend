-- 2026-05-25 — quiz.lesson_id 레거시 컬럼 제거
--
-- 배경:
--   강의/교안 분리 리팩토링(8c21bb9) 이후 Quiz.material 의 FK 컬럼명이
--   lesson_material_id 로 정리됐다. lecture_material 에 lesson_id 를 추가하는
--   과정에서 quiz 에도 lesson_material_id 가 ADD 됐지만 옛 lesson_id NOT NULL
--   컬럼이 잔존했고, 엔티티는 lesson_material_id 만 INSERT 하므로 옛 lesson_id
--   에 null 이 들어가 NOT NULL 위반으로 POST /api/quiz 가 500 으로 실패했다.
--
-- 적용 환경: Supabase (PostgreSQL). Supabase SQL Editor 또는 psql 에서 실행.
-- 안전성: quiz 0행 상태에서 적용 검증됨. DROP COLUMN 이므로 IF EXISTS 로 멱등.
--
-- 적용 전 검증 (선택):
--   SELECT column_name, is_nullable
--   FROM information_schema.columns
--   WHERE table_schema = 'public' AND table_name = 'quiz'
--   ORDER BY ordinal_position;
--   -- lesson_id 와 lesson_material_id 가 둘 다 보여야 한다 (현 시점 기준).

ALTER TABLE quiz DROP COLUMN IF EXISTS lesson_id;

-- 적용 후 검증:
--   SELECT column_name, is_nullable
--   FROM information_schema.columns
--   WHERE table_schema = 'public' AND table_name = 'quiz'
--   ORDER BY ordinal_position;
--   -- lesson_id 가 더 이상 보이지 않아야 한다. lesson_material_id (bigint, NO) 만 남는다.
