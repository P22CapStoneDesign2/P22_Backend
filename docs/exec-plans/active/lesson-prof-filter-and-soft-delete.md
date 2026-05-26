# [ACTIVE] 강의 PROF 필터링 및 소프트 삭제

- **시작일**: 2026-05-26
- **브랜치**: dev

## 목표

두 가지 문제를 단계적으로 해결한다.

1. **1단계 — PROF 강의 목록 필터링**: `GET /api/lessons` 호출 시 PROF 역할 사용자가 자신이 생성하지 않은 강의까지 조회되는 문제를 수정한다. PROF는 본인이 생성한 강의만 조회해야 한다.

2. **2단계 — 소프트 삭제**: 강의(`Lesson`)·교안(`LessonMaterial`) 삭제 시 FK 참조로 인해 하드 삭제가 불가능하고, 하위 데이터(교안, 퀴즈)가 연쇄 삭제되는 문제를 해결한다. `deleted` 플래그를 추가하고, 삭제된 리소스는 조회에서 제외한다. (Quiz는 이미 소프트 삭제 적용됨)

---

## 1단계 수용 기준

- [x] `GET /api/lessons` — PROF 호출 시 본인 강의만 반환
- [x] `GET /api/lessons` — USER/ADMIN 호출 시 전체 목록 반환 (기존 동작 유지)
- [x] `LessonController.getAll()`에 `@AuthenticationPrincipal` 추가 (auth=✅ 계약 이행)
- [x] `LessonRepository`에 `findAllByCreatedBy_Id` 추가
- [x] `LessonService.getAll()`이 `userId`, `role` 파라미터를 받아 분기 처리
- [x] `docs/API.md` — `GET /api/lessons` 권한 컬럼에 PROF 필터 명시
- [x] `./gradlew build` 통과

---

## 2단계 수용 기준

- [x] `Lesson` — `deleted`, `deletedAt` 필드 추가 + `@SQLDelete` + `@SQLRestriction`
- [x] `LessonMaterial` — 동일하게 소프트 삭제 적용
- [x] `LessonService.delete()` — 소프트 삭제로 변경 (하위 교안·퀴즈 연쇄)
- [x] `LessonMaterialService.delete()` — 소프트 삭제로 변경 (하위 퀴즈 연쇄)
- [x] DB 마이그레이션 SQL 작성 (Flyway V4 — V3는 DB에 이미 적용된 미아 마이그레이션)
- [x] 삭제된 강의/교안은 목록·단건 조회에서 제외됨 (`@SQLRestriction` 으로 자동)
- [x] `docs/API.md` 갱신 — 삭제 동작 명시 (소프트 삭제)
- [x] `./gradlew build` 통과

---

## 의사결정 로그

### 2026-05-26

**PROF 강의 필터 범위**: `GET /api/lessons` 목록에만 적용. 단건 조회(`GET /api/lessons/{id}`)는 현행 유지 (ID를 알아야 호출 가능하므로 노출 위험 낮음).

**소프트 삭제 방식**: Hibernate `@SQLDelete` + `@SQLRestriction` 패턴 사용 (Quiz에 이미 적용된 패턴과 통일). JPA `delete()` 호출 시 UPDATE 쿼리가 발행되며, 모든 조회 쿼리에 `WHERE deleted = false` 가 자동 추가됨.

**LessonMaterial 삭제 시 Quiz 처리**: `LessonMaterial`을 소프트 삭제해도 FK 참조 Quiz는 남는다. `Quiz.@SQLRestriction`이 이미 걸려 있으므로 Quiz도 별도로 소프트 삭제 처리 필요. `LessonMaterialService.delete()`에서 해당 교안의 미삭제 Quiz를 함께 소프트 삭제한다.

**Lesson 삭제 시 LessonMaterial, Quiz 처리**: `LessonService.delete()`에서 연결된 LessonMaterial 전체를 소프트 삭제하고, 각 교안의 Quiz도 연쇄 소프트 삭제.
