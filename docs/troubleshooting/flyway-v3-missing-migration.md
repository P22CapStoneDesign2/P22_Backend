# Flyway V3 미아 마이그레이션 — `applied migration not resolved locally`

- **발생일**: 2026-05-26
- **발견 경로**: `./gradlew build` 중 `contextLoads()` 통합 테스트 실패
- **관련 파일**: `src/main/resources/application-local.yaml`, `src/main/resources/db/migration/`

## 증상

```
FlywayValidateException: Validate failed: Migrations have failed validation
Detected applied migration not resolved locally: 3.
If you removed this migration intentionally, run repair to mark the migration as deleted.
```

또는 (V3 파일이 존재하지만 내용이 다를 때):

```
Migration checksum mismatch for migration version 3
-> Applied to database : -115794676
-> Resolved locally    : -1204171785
```

## 원인

Supabase `flyway_schema_history` 테이블에 V3 마이그레이션이 적용된 기록이 남아 있지만, 해당 SQL 파일이 로컬 파일시스템(Git)에 존재하지 않는 상태. V3 마이그레이션이 Flyway 외부(예: Supabase SQL Editor)에서 직접 적용된 후 파일이 커밋되지 않은 것으로 추정됨.

`validate-on-migrate: true` 설정 때문에 Flyway가 새 마이그레이션 실행 전 기존 적용 이력을 검증하면서 이 불일치를 오류로 처리함.

## 조치

`application-local.yaml`에 다음 설정 추가:

```yaml
spring:
  flyway:
    ignore-migration-patterns: "*:missing"
```

이후 Flyway V4 (`V4__add_soft_delete_to_lesson_and_material.sql`)가 정상 적용됨.

## 검증

`./gradlew build` → 98 tests passed, BUILD SUCCESSFUL

## 교훈 / 재발 방지

- Flyway 관리 환경에서 DB를 직접 수정할 경우 반드시 해당 내용을 SQL 파일로 커밋하고 Flyway 버전을 부여해야 함
- `ignore-migration-patterns: "*:missing"` 은 광범위한 억제이므로, V3 내용을 복원할 수 있다면 정확한 파일로 대체하고 이 설정을 제거하는 것이 바람직함
- V3 DB 체크섬: `-115794676` (복원 시 참고)
