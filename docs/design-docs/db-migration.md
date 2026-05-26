# DB 마이그레이션 — Flyway baseline + `ddl-auto: validate`

> 상태: ✅ 확정 (2026-05-25)
> 대상: `src/main/resources/db/migration/`, `spring.flyway.*`, `spring.jpa.hibernate.ddl-auto`

## 결정

EQH 백엔드의 DB 스키마 변경은 **Flyway 마이그레이션 SQL 파일**을 진본으로 한다. Hibernate `ddl-auto` 는 `validate` 로 고정해 엔티티-DB 불일치 시 부팅을 실패시킨다.

## 동기 — 왜 이 설계인가

`ddl-auto: update` 는 다음을 **자동 반영하지 않는다.**

- 기존 행이 있는 테이블에 NOT NULL 컬럼 추가
- 기존 NOT NULL 제약 제거
- 컬럼 RENAME
- 사용하지 않는 컬럼 DROP
- FK 정리

실제 사고 사례:

- [lecture-material-content-not-null](../troubleshooting/lecture-material-content-not-null.md) — NOT NULL 잔존
- [lecture-material-missing-lesson-id](../troubleshooting/lecture-material-missing-lesson-id.md) — NOT NULL 컬럼 추가 실패
- [quiz-legacy-lesson-id-not-null](../troubleshooting/quiz-legacy-lesson-id-not-null.md) — RENAME 누락으로 옛 컬럼 잔존

엔티티만 보고 자동으로 따라잡지 않으므로 진본을 코드(SQL 파일)로 옮기고, `validate` 로 어긋남을 부팅 시 즉시 노출시킨다.

## 구성

### 의존성 (`build.gradle.kts`)

```kotlin
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
```

> Spring Boot 3.5.x 는 Flyway 10 을 기본 사용. PostgreSQL 은 별도 `flyway-database-postgresql` 모듈 필요.

### 설정 (`application-local.yaml`)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 1
    baseline-description: "Existing schema captured by ddl-auto:update before Flyway adoption"
    validate-on-migrate: true
```

### 파일 규약

- 경로: `src/main/resources/db/migration/`
- 네이밍: `V{version}__{snake_case_description}.sql` (예: `V2__drop_quiz_legacy_lesson_id.sql`)
- 버전: 단조 증가. 한 번 머지된 V 파일의 내용은 절대 수정하지 않는다 — 후속 V 파일로 정정한다.
- DDL 은 가능한 `IF EXISTS` / `IF NOT EXISTS` 로 멱등하게 작성한다.
- 데이터 시드(예: `src/main/resources/sql/insert-admin.sql`)는 Flyway 영역 밖에 둔다 — 환경별 1회성 작업.

### Baseline 의미

이번 도입 시점에 운영 DB(Supabase)는 이미 `ddl-auto: update` 가 만든 스키마를 가지고 있다. `baseline-on-migrate: true` + `baseline-version: 1` 로 **그 시점을 V1 (BASELINE) 으로 마킹**한다. 그 결과:

1. 첫 부팅 시 Flyway 가 `flyway_schema_history` 테이블 생성 후 `V1 / BASELINE` 행만 기록.
2. `V2__drop_quiz_legacy_lesson_id.sql` 부터 실제 마이그레이션으로 실행.
3. 이후 모든 변경은 `V3`, `V4`, … 로 추가.

`V1__*.sql` 파일은 두지 않는다. baseline 은 "기록만 남기는 점프 표시" 이지 실행 가능한 SQL 이 아니다.

## 워크플로

신규 스키마 변경:

1. 엔티티 변경 + 그에 대응하는 `V{n}__*.sql` 작성. 같은 PR 에 묶는다.
2. 로컬에서 `./gradlew bootRun` — Flyway 가 자동 적용, `validate` 가 통과해야 부팅된다.
3. 운영 환경 배포 시 Flyway 가 동일 V 파일을 순차 적용.

스키마 변경 정정:

- 머지된 V 파일은 수정 금지. 정정용 `V{n+1}__fix_*.sql` 을 새로 작성한다.
- Flyway 체크섬 검증 실패 시 — 머지 이후 파일을 손댄 것이다. revert 하거나 새 V 파일로 정정.

## 비검증 항목 (validate 한계)

`ddl-auto: validate` 는 다음을 검사한다.

- 테이블 존재
- 컬럼 존재
- 컬럼 데이터 타입 호환

다음은 **검사하지 않으므로 마이그레이션 SQL 로 직접 관리해야 한다**.

- NOT NULL 제약
- DEFAULT 값
- 인덱스
- FK 제약

NOT NULL 어긋남이 운영에서 다시 발생하면 트러블슈팅에 기록하고, V 파일로 정정한다.

## 관련 문서

- [troubleshooting/lecture-material-missing-lesson-id.md](../troubleshooting/lecture-material-missing-lesson-id.md)
- [troubleshooting/quiz-legacy-lesson-id-not-null.md](../troubleshooting/quiz-legacy-lesson-id-not-null.md)
