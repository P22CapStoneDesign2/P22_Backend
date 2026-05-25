# `lecture_material.lesson_id` 컬럼 누락으로 교안 생성 500

- **발생일**: 2026-05-25
- **발견 경로**: 프론트엔드 연동 테스트 (`POST /api/lessons/{lessonId}/materials`)
- **관련 파일**:
  - `domain/lesson/entity/LessonMaterial.java`
  - `domain/quiz/entity/Quiz.java`

## 증상

교안(LessonMaterial) 생성 시 500 응답.

```
ERROR: null value in column "lesson_id" of relation "lecture_material" violates not-null constraint
INSERT 로그: insert into lecture_material (created_at, professor_id, content, title, updated_at)
```

JPA 코드는 `LessonMaterial.builder().lesson(lesson)...build()` 로 `lesson_id` 를 채우는데, 실제 SQL에는 컬럼 자체가 빠져 있었다.

## 원인

엔티티 분리 리팩토링(8c21bb9 "교안-강의 분리") 이후 `LessonMaterial.lesson` (`@JoinColumn(name = "lesson_id", nullable = false)`) 가 추가됐지만 Supabase(PostgreSQL) `lecture_material` 테이블에는 `lesson_id` 컬럼이 존재하지 않았다.

`spring.jpa.hibernate.ddl-auto: update` 는 **기존 행이 있는 테이블에 `NOT NULL` 컬럼을 추가하지 못한다.** 기본값 없는 NOT NULL 컬럼을 추가하면 기존 행이 제약을 위반하므로 PostgreSQL 이 거부하고, Hibernate 는 조용히 스킵한 채 부팅을 계속한다. 그 결과 엔티티만 `lesson_id` 를 알고, 실제 테이블은 모르는 상태가 됐다.

이전 `lecture_material.content NOT NULL` 사건(`docs/troubleshooting/lecture-material-content-not-null.md`)과 같은 패턴 — `ddl-auto: update` 는 NOT NULL 관련 변경을 자동 반영하지 않는다.

부수적으로 `Quiz.material` 의 FK 컬럼명이 분리 이전 명명(`lesson_id`) 그대로여서 새로 추가될 `lecture_material.lesson_id` 와 의미가 충돌했다 → 컬럼명을 `lesson_material_id` 로 정리.

## 조치

1. **코드 변경**

   `Quiz.java` JoinColumn 명을 의미에 맞게 변경.

   ```java
   @JoinColumn(name = "lesson_material_id", nullable = false)
   private LessonMaterial material;
   ```

2. **DB 스키마 정리**

   `quiz` 테이블이 `lecture_material` 을 FK 참조하므로 `TRUNCATE` 만으로는 안 되고 CASCADE 필요. 개발 데이터 전량 삭제 후 컬럼 정리.

   ```sql
   -- 강의/교안/퀴즈 트리 전체 비우기
   TRUNCATE TABLE lesson, lecture_material CASCADE;

   -- lecture_material 에 누락된 FK 컬럼 추가
   ALTER TABLE lecture_material
     ADD COLUMN lesson_id BIGINT NOT NULL
     REFERENCES lesson(id);

   -- quiz FK 컬럼명 정리 (lesson_id 의 의미가 강의가 아닌 교안임을 명확히)
   ALTER TABLE quiz RENAME COLUMN lesson_id TO lesson_material_id;
   ```

3. **문서 갱신**

   - `docs/generated/db-schema.md` 의 `quiz.lesson_id` → `lesson_material_id` 반영.

## 검증

```sql
SELECT column_name, is_nullable, data_type
FROM information_schema.columns
WHERE table_name = 'lecture_material'
ORDER BY ordinal_position;
-- lesson_id (bigint, NO) 가 존재해야 함

SELECT column_name, is_nullable, data_type
FROM information_schema.columns
WHERE table_name = 'quiz'
ORDER BY ordinal_position;
-- lesson_material_id (bigint, NO) 가 존재해야 함
```

이후 `POST /api/lessons` → `POST /api/lessons/{lessonId}/materials` 흐름 호출 → 201 정상.

## 교훈 / 재발 방지

- `ddl-auto: update` 는 **NOT NULL 컬럼 추가, 기존 NOT NULL 해제, 컬럼 rename, FK 정리 등 제약 관련 변경을 자동 반영하지 않는다.** 스키마에 직접 반영하지 않으면 엔티티-DB 가 어긋난 채 부팅된다.
- 엔티티 분리/리팩토링 PR 에는 항상 **Supabase 에서 실행할 SQL 마이그레이션** 을 PR 설명에 함께 적는다.
- 분리 이후 FK 컬럼명이 의미와 일치하는지 즉시 점검한다 (예: `Quiz.material` 의 FK 는 `lesson_id` 가 아니라 `lesson_material_id` 여야 의미가 맞는다).
- 운영 환경 도입 전에 Flyway 등 마이그레이션 도구를 붙여 `ddl-auto: validate` 로 전환할 것.
