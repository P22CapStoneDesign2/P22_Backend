# RBAC 권한 체계 설계 결정

> 상태: ✅ 확정 | 대상: 전체 도메인

---

## Role 정의

| Role | 설명 | 주요 권한 |
|------|------|-----------|
| `PROF` | 강사(교수) | 교안 생성·수정·삭제, 퀴즈 세트·문제 생성·관리 |
| `USER` | 학생 | 교안 조회, 퀴즈 제출, 오답 조회 |
| `ADMIN` | 관리자 | 모든 리소스 생성·수정·삭제 |

---

## 결정 1 — Role 정의와 Role 강제 분리

**결정**:
- `Role` enum → `domain/user/enums/Role.java` (비즈니스 데이터)
- 접근 제어 규칙 → `global/security/SecurityConfig.java` (횡단 관심사)

**이유**: domain 패키지가 security 패키지에 의존하지 않도록 경계를 유지.

---

## 결정 2 — 소유자 검증 패턴

**결정**: 교안·퀴즈의 소유자 검증은 Controller의 `@PreAuthorize` + Service의 `isOwner(Long id, Long userId)`.

**규칙**:
- Service `isOwner`는 반드시 `Long userId`만 받음 — `CustomUserDetails` 파라미터 금지
- Controller SpEL에서 `principal.userId`를 꺼내 전달

```java
// 올바른 패턴
@PreAuthorize("(hasRole('PROF') and @quizService.isOwner(#quizId, principal.userId)) or hasRole('ADMIN')")
public ResponseEntity<?> update(@PathVariable Long quizId, ...) { ... }
```

---

## 결정 3 — 퀴즈 접근 제어 방식

**결정**: `/api/quiz/**`는 SecurityConfig에서 `authenticated()`만 설정, 세부 권한은 메서드 레벨 `@PreAuthorize`.

**이유**: 같은 URL에서 역할별 동작이 다름 (USER는 조회·제출, PROF는 생성·수정·삭제).

---

## 엔드포인트별 권한 매트릭스

| 엔드포인트 | PROF | USER | ADMIN |
|-----------|------|------|-------|
| `POST /api/lessons` | ✅ | ❌ | ✅ |
| `GET /api/lessons/**` | ✅ | ✅ | ✅ |
| `PUT/DELETE /api/lessons/{id}` | ✅ (본인) | ❌ | ✅ |
| `POST /api/quiz` | ✅ | ❌ | ✅ |
| `GET /api/quiz/**` | ✅ | ✅ | ✅ |
| `PUT/DELETE /api/quiz/{id}` | ✅ (본인) | ❌ | ✅ |
| `POST /api/quiz/{id}/questions` | ✅ (본인) | ❌ | ✅ |
| `POST /api/quiz/{id}/submit` | ❌ | ✅ | ❌ |
| `GET /api/quiz/wrong-answers` | ❌ | ✅ | ❌ |
| `GET /api/admin/**` | ❌ | ❌ | ✅ |
