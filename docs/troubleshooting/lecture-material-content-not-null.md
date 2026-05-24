# `lecture_material.content` NOT NULL 제약으로 교안 생성 500

- **발생일**: 2026-05-24
- **발견 경로**: 프론트엔드 연동 테스트 (ACTIVE PROF 계정으로 POST /api/lessons)
- **관련 파일**: `domain/lesson/entity/Lesson.java`

## 증상

ACTIVE PROF 계정으로 `POST /api/lessons` 요청 시 500 응답 반환.
`GET /api/lessons` (목록 조회)는 정상 동작.

## 원인

`Lesson` 엔티티의 `description` 필드는 `@Column(name = "content", columnDefinition = "TEXT")`로 선언되어 nullable이다. 그러나 Supabase(PostgreSQL)의 실제 `lecture_material.content` 컬럼에는 NOT NULL 제약이 걸려 있었다.

`spring.jpa.hibernate.ddl-auto: update`는 컬럼을 추가하거나 타입을 변경할 수는 있지만, 기존 NOT NULL 제약을 자동으로 제거하지 않는다. 테이블이 처음 생성된 시점의 DDL 상태가 그대로 남아 있어, description을 null로 보내면 PostgreSQL NOT NULL 위반 → 500이 발생했다.

## 조치

Supabase SQL 에디터에서 직접 제약 제거:

```sql
ALTER TABLE lecture_material ALTER COLUMN content DROP NOT NULL;
```

엔티티 코드 변경 없음 — 엔티티 선언은 이미 nullable이었으므로 DB만 맞춤.

## 검증

제약 제거 후 ACTIVE PROF 계정으로 `POST /api/lessons` 요청 → 201 정상 응답 확인.

```sql
-- 제거 후 확인 쿼리
SELECT column_name, is_nullable
FROM information_schema.columns
WHERE table_name = 'lecture_material' AND column_name = 'content';
-- is_nullable = YES 여야 함
```

## 교훈 / 재발 방지

- `ddl-auto: update`는 NOT NULL 제약 제거를 하지 않는다. 엔티티에서 `nullable = false`를 제거해도 DB 제약은 그대로 남는다.
- 실제 DB 제약과 엔티티 선언이 어긋난 경우 `information_schema.columns`로 직접 확인한다.
- 컬럼을 nullable로 바꾸는 변경이 있으면 Flyway/수동 마이그레이션 SQL로 `ALTER COLUMN ... DROP NOT NULL`을 함께 실행해야 한다.
