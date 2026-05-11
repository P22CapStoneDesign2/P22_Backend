# 기술 부채 추적

> 알려진 기술 부채 목록입니다. 새 기능 개발 전 여기를 확인하세요.
> 해결 시 ✅ 완료로 이동하고 날짜와 PR을 기록합니다.

---

## 🔴 높음 (High)

| ID | 항목 | 위치 | 설명 |
|----|------|------|------|
| TD-001 | 서비스 레이어 단위 테스트 부재 | `domain/*/service/` | CLAUDE.md에 필수로 명시되어 있으나 미작성. `UserAuthService`, `QuizService`, `LessonService` 우선 |

---

## 🟡 중간 (Medium)

| ID | 항목 | 위치 | 설명 |
|----|------|------|------|
| TD-002 | isOwner 중복 DB 조회 | `LessonService`, `QuizService` | `@PreAuthorize` isOwner + update/delete에서 같은 id로 `findById` 2회 호출 |
| TD-003 | update/delete 서비스 레이어 소유자 검증 없음 | `LessonService`, `QuizService` | 소유자 검증이 `@PreAuthorize`에만 존재. Security 컨텍스트 우회 시 서비스 방어 없음 |

---

## 🟢 낮음 (Low)

| ID | 항목 | 위치 | 설명 |
|----|------|------|------|
| TD-004 | API.md role 값 불일치 | `docs/API.md` | 응답 예시가 `STUDENT`/`PROFESSOR`로 표기되어 있으나 실제 enum은 `USER`/`PROF` |
| TD-005 | `User` 엔티티 `@AllArgsConstructor` 패턴 불일치 | `domain/user/entity/User.java` | 다른 엔티티와 달리 `@AllArgsConstructor(access = PRIVATE)` 없이 `@Builder` 직접 사용 |

---

## ✅ 해결 완료

| ID | 항목 | 해결일 | PR |
|----|------|--------|----|
| TD-006 | CLAUDE.md 패키지명 오류 (`eqz_backend`) | 2026-05-11 | refactor/arch_minsik |
| TD-007 | Service 레이어가 `CustomUserDetails`에 의존 | 2026-05-11 | refactor/arch_minsik |
