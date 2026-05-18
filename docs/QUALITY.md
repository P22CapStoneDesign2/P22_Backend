# 품질 등급

> 도메인·레이어별 현재 품질 상태입니다.
> 배경색 기준: 🟢 양호 / 🟡 개선 필요 / 🔴 취약

마지막 갱신: 2026-05-11

---

## 도메인별 품질

| 도메인 | 테스트 | 아키텍처 | 문서화 | 종합 |
|--------|--------|---------|--------|------|
| `user` | 🔴 없음 | 🟢 ArchUnit 통과 | 🟢 설계 결정 문서화 | 🟡 |
| `quiz` | 🔴 없음 | 🟢 ArchUnit 통과 | 🟢 스펙 문서화 | 🟡 |
| `lesson` | 🔴 없음 | 🟢 ArchUnit 통과 | 🟢 스펙 문서화 | 🟡 |
| `global` | 🔴 없음 | 🟢 ArchUnit 통과 | 🟢 설계 결정 문서화 | 🟡 |

---

## 레이어별 품질

| 레이어 | 커버리지 | 규칙 강제 | 비고 |
|--------|---------|----------|------|
| Controller | 🔴 없음 | 🟢 ArchUnit | @PreAuthorize 동작 미검증 |
| Service | 🔴 없음 | 🟢 ArchUnit | 단위 테스트 필수 (TD-001) |
| Repository | 🟡 암묵적 | 🟢 ArchUnit | Spring Data JPA 기본 동작 의존 |

---

## 개선 우선순위

1. **TD-001**: `UserAuthService`, `QuizService`, `LessonService` 단위 테스트 작성
2. **TD-002**: `isOwner` 중복 DB 조회 제거
3. **TD-004**: `API.md` role 값 수정 (`STUDENT` → `USER`, `PROFESSOR` → `PROF`)
