# [Active] 교수 회원가입 승인 워크플로

- **시작일**: 2026-05-21
- **브랜치**: feat/lesson_sik (Day 2) — Day 1과 같은 브랜치에서 이어 진행. 분리 필요 시 후속 결정.

## 목표

PROF 회원가입을 즉시 활성화하지 않고 ADMIN 수락 후 활성화한다.
- 가입 시 토큰은 그대로 발급 (사용자 결정).
- PROF 전용 기능 API는 PENDING이면 차단.
- REJECTED된 이메일은 재가입을 차단하되 DB 행은 유지. ADMIN이 추후 상태를 변경할 수 있다.

## 수용 기준

- [ ] `UserStatus` enum 추가 (PENDING / ACTIVE / REJECTED)
- [ ] `User.status` 컬럼 추가, 기본값 ACTIVE, 기존 행은 ACTIVE로 백필
- [ ] PROF 가입(`/api/auth/profsignup`) 시 status=PENDING으로 저장하고 토큰 발급은 정상 수행
- [ ] PROF 가입 응답에 `status: "PENDING"` 포함
- [ ] USER 카카오 가입은 status=ACTIVE로 그대로 동작
- [ ] PROF 전용/소유자 API에서 PENDING이면 403 `PROF_NOT_APPROVED`
- [ ] REJECTED 이메일로 `/api/auth/profsignup` 재시도 시 409 `EMAIL_REJECTED`
- [ ] `GET /api/admin/users/pending` — 승인 대기 PROF 목록
- [ ] `POST /api/admin/users/{id}/approve` — ACTIVE로 변경
- [ ] `POST /api/admin/users/{id}/reject` — REJECTED로 변경
- [ ] `PATCH /api/admin/users/{id}/status` — ADMIN 임의 상태 변경 (REJECTED → ACTIVE 등 후속 조정)
- [ ] `GET /api/users/me` 응답에 `status` 포함
- [ ] `docs/API.md`, `docs/ARCHITECTURE.md`, `docs/SECURITY.md`, `docs/generated/db-schema.md` 갱신
- [ ] 단위 테스트 + 통합 테스트 (PENDING 차단 / APPROVED 후 정상 동작)
- [ ] `./gradlew build` 통과

## API 추가/변경

### 변경

| Method | URL | 변경점 |
|--------|-----|--------|
| POST | `/api/auth/profsignup` | 응답 데이터에 `status: "PENDING"` 추가, REJECTED 이메일 재시도 409 |
| GET | `/api/users/me` | 응답에 `status` 필드 추가 |

### 신규

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| GET | `/api/admin/users/pending` | ADMIN | 승인 대기 PROF 목록 (페이지네이션) |
| POST | `/api/admin/users/{id}/approve` | ADMIN | ACTIVE로 변경 |
| POST | `/api/admin/users/{id}/reject` | ADMIN | REJECTED로 변경 |
| PATCH | `/api/admin/users/{id}/status` | ADMIN | 임의 상태 변경 (`status` body) |

## ErrorCode 추가

| 코드 | Status | 메시지 |
|------|--------|--------|
| `PROF_NOT_APPROVED` | 403 | 교수 계정 승인 대기 중입니다. |
| `EMAIL_REJECTED` | 409 | 가입이 거절된 이메일입니다. 관리자에게 문의해 주세요. |

## DB 변경

### users 테이블 컬럼 추가

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `status` | VARCHAR(10) | PENDING / ACTIVE / REJECTED, NOT NULL, DEFAULT 'ACTIVE' |

마이그레이션:
- `ALTER TABLE users ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'`
- 신규 PROF 가입만 PENDING으로 저장

## 보안 적용 방식

- `CustomUserDetails`에 `isActive()` getter 추가 — `user.getStatus() == UserStatus.ACTIVE` 반환
- `@PreAuthorize` 표현식에 조건 추가:
  ```
  hasRole('PROF') and principal.active
  ```
- 영향받는 endpoint:
  - `POST /api/lessons`, `PUT/DELETE /api/lessons/{id}` (PROF/PROF본인)
  - `POST /api/quiz`, `PUT/DELETE /api/quiz/{quizId}` (PROF/PROF본인)
  - 퀴즈 문제 CRUD 전반
  - Day 1에 추가된 enrollment approve/reject (PROF본인)
- ADMIN은 status 영향 없음 (ADMIN은 항상 ACTIVE 가정)
- USER 역할 endpoint(`/api/lessons/{id}/enrollments` POST 등)는 status 영향 없음 (USER는 PENDING이 없음)

## 의사결정 로그

### 2026-05-21
- 가입 시 토큰을 발급하되 기능만 제한 (사용자 결정). 가입 직후 사용자가 `GET /api/users/me`로 PENDING 상태를 확인할 수 있다.
- USER 카카오 가입은 PENDING을 거치지 않음.
- REJECTED 이메일은 DB에 행을 유지 (UNIQUE 이메일 제약 + status=REJECTED 검사로 재가입 차단). 영구 차단이 아니라 ADMIN이 status를 변경하면 재활용 가능.
- `principal.active`를 `CustomUserDetails`에 추가하는 것은 보안 객체 책임 안 (이미 User 정보를 보유). domain 계층 의존 규칙 위반 아님.
- ArchUnit 규칙 변경 없음 (`UserStatus` enum은 `domain/user/enums/`에 위치).

## 테스트 케이스

### UserSignupServiceTest (확장)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `profSignup_savesAsPending` | PROF 가입 | status=PENDING으로 저장, 토큰 발급 |
| 2 | `profSignup_rejectedEmail` | REJECTED 이메일 재가입 | `EMAIL_REJECTED` 409 |
| 3 | `userSignup_savesAsActive` | 카카오 USER 가입 | status=ACTIVE |

### AdminUserServiceTest (신규)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `approve_pendingProf` | PENDING PROF approve | ACTIVE로 변경 |
| 2 | `reject_pendingProf` | PENDING PROF reject | REJECTED로 변경 |
| 3 | `changeStatus_rejectedToActive` | REJECTED → ACTIVE 변경 | 정상 변경 |
| 4 | `listPending_returnsPendingProfs` | pending 목록 조회 | PENDING PROF만 반환 |

### 통합 테스트 (PENDING 차단)

| # | 시나리오 | 기대 결과 |
|---|----------|----------|
| 1 | PENDING PROF의 `POST /api/lessons` | 403 `PROF_NOT_APPROVED` |
| 2 | APPROVED 후 동일 호출 | 201 |
| 3 | PENDING PROF의 `POST /api/quiz` | 403 |
| 4 | PENDING PROF의 enrollment approve | 403 |

## 작업 순서

1. 본 계획 문서 사용자 승인
2. `docs/API.md` 갱신 (signup/me 응답 변경, admin endpoint 추가)
3. `UserStatus` enum, `User.status` 컬럼 추가, 백필
4. 회원가입 흐름 변경 + REJECTED 재가입 차단
5. `CustomUserDetails.isActive` 추가, `@PreAuthorize` 갱신
6. AdminUserController/Service 추가
7. 테스트 추가, build 통과
8. 사용자 검토 → 머지 → `completed/`로 이동
