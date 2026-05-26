# `quiz.lesson_id` 레거시 NOT NULL 컬럼 잔존으로 퀴즈 생성 500

- **발생일**: 2026-05-25
- **발견 경로**: 프론트엔드 연동 테스트 (`POST /api/quiz`)
- **관련 파일**:
  - `domain/quiz/entity/Quiz.java`
  - `src/main/resources/db/migration/V2__drop_quiz_legacy_lesson_id.sql`
- **관련 이슈**: [lecture-material-missing-lesson-id.md](lecture-material-missing-lesson-id.md) 의 후속 정리

## 증상

`POST /api/quiz` 요청 시 500 응답.

```
ERROR: null value in column "lesson_id" of relation "quiz" violates not-null constraint
```

엔티티는 `lesson_material_id` 만 INSERT 에 포함하는데, DB 의 옛 `lesson_id NOT NULL` 컬럼에 값이 들어가지 않아 PostgreSQL 이 거부했다.

## 원인

이전 트러블슈팅 [lecture-material-missing-lesson-id.md](lecture-material-missing-lesson-id.md) 에서 권고한 SQL 은 `ALTER TABLE quiz RENAME COLUMN lesson_id TO lesson_material_id` 였지만, 실제 운영 DB(Supabase) 에는 **RENAME 대신 ADD 만 적용되어** `lesson_id` 와 `lesson_material_id` 가 동시에 NOT NULL 로 존재했다.

`Quiz.material` 의 JoinColumn 은 `lesson_material_id` 이므로 엔티티 INSERT 에서 `lesson_id` 컬럼은 누락된다. 잔존하는 `lesson_id NOT NULL` 제약이 그 누락을 위반으로 잡아 500 으로 이어졌다.

`spring.jpa.hibernate.ddl-auto: update` 는 사용하지 않는 컬럼을 자동 제거하지 않으므로, 이 잔존 컬럼은 영원히 살아남는다.

진단 (information_schema 발췌):

```text
quiz | lesson_material_id | NO | bigint
quiz | lesson_id          | NO | bigint   ← 잔존 레거시
```

## 조치

`quiz` 테이블에서 레거시 `lesson_id` 컬럼 제거. (행 수 0 인 상태에서 적용.)

```sql
ALTER TABLE quiz DROP COLUMN IF EXISTS lesson_id;
```

Flyway 마이그레이션 파일로 커밋:

- `src/main/resources/db/migration/V2__drop_quiz_legacy_lesson_id.sql`

엔티티/문서는 이미 `lesson_material_id` 기준으로 정리되어 있으므로 코드 변경 없음.

## 검증

```sql
-- 컬럼 확인
SELECT column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'quiz'
ORDER BY ordinal_position;
-- lesson_id 가 더 이상 보이지 않아야 한다.
```

이후 다음 흐름이 정상이어야 한다.

1. `POST /api/lessons` → 201
2. `POST /api/lessons/{lessonId}/materials` → 201
3. `POST /api/quiz` (`materialId` 포함) → 201
4. `GET /api/quiz?materialId={id}` → 200, 방금 생성한 퀴즈 노출
5. `POST /api/quiz/{quizId}/questions` → 201

## 교훈 / 재발 방지

- 컬럼 RENAME 을 권고한 SQL 이 실제 환경에서 ADD 로 적용된 사례. 마이그레이션 SQL 은 **`src/main/resources/db/migration/V{n}__*.sql` 에 파일로 커밋** 해서 단일 진본을 유지한다 (이번에 Flyway 도입 — [design-docs/db-migration.md](../design-docs/db-migration.md)).
- `ddl-auto: update` 는 사용하지 않는 컬럼을 자동 제거하지 않으므로, FK 의미가 바뀌는 리팩토링에서는 옛 컬럼 잔존을 명시적으로 확인해야 한다.
- ~~운영 도입 전 `ddl-auto: validate` + Flyway 로 전환할 것. (별도 안건)~~ → **2026-05-25 동일 PR 에서 도입 완료.**
