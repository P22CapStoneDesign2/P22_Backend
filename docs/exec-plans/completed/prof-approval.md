# [Completed] 교수 회원가입 승인 워크플로

- **시작일**: 2026-05-21
- **완료일**: 2026-05-24
- **브랜치**: feature/login_minsik → dev 머지 → origin/dev push

## 목표

PROF 회원가입을 즉시 활성화하지 않고 ADMIN 수락 후 활성화한다.
- 가입 시 토큰은 그대로 발급 (사용자 결정).
- PROF 전용 기능 API는 PENDING이면 차단.
- REJECTED된 이메일은 재가입을 차단하되 DB 행은 유지. ADMIN이 추후 상태를 변경할 수 있다.

## 수용 기준

- [x] `UserStatus` enum 추가 (PENDING / ACTIVE / REJECTED)
- [x] `User.status` 컬럼 추가, 기본값 ACTIVE, 기존 행은 ACTIVE로 백필
- [x] PROF 가입(`/api/auth/profsignup`) 시 status=PENDING으로 저장하고 토큰 발급은 정상 수행
- [x] PROF 가입 응답에 `status: "PENDING"` 포함
- [x] USER 카카오 가입은 status=ACTIVE로 그대로 동작
- [x] PROF 전용/소유자 API에서 PENDING이면 403 `PROF_NOT_APPROVED`
- [x] REJECTED 이메일로 `/api/auth/profsignup` 재시도 시 409 `EMAIL_REJECTED`
- [x] `GET /api/admin/users/pending` — 승인 대기 PROF 목록
- [x] `POST /api/admin/users/{id}/approve` — ACTIVE로 변경
- [x] `POST /api/admin/users/{id}/reject` — REJECTED로 변경
- [x] `PATCH /api/admin/users/{id}/status` — ADMIN 임의 상태 변경 (REJECTED → ACTIVE 등 후속 조정)
- [x] `GET /api/users/me` 응답에 `status` 포함
- [x] `docs/API.md`, `docs/ARCHITECTURE.md`, `docs/SECURITY.md`, `docs/generated/db-schema.md` 갱신
- [x] 단위 테스트 + 통합 테스트 (PENDING 차단 / APPROVED 후 정상 동작)
- [x] `./gradlew build` 통과

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

## 의사결정 로그

### 2026-05-21
- 가입 시 토큰을 발급하되 기능만 제한 (사용자 결정). 가입 직후 사용자가 `GET /api/users/me`로 PENDING 상태를 확인할 수 있다.
- USER 카카오 가입은 PENDING을 거치지 않음.
- REJECTED 이메일은 DB에 행을 유지 (UNIQUE 이메일 제약 + status=REJECTED 검사로 재가입 차단). 영구 차단이 아니라 ADMIN이 status를 변경하면 재활용 가능.
- `principal.active`를 `CustomUserDetails`에 추가하는 것은 보안 객체 책임 안 (이미 User 정보를 보유). domain 계층 의존 규칙 위반 아님.
- ArchUnit 규칙 변경 없음 (`UserStatus` enum은 `domain/user/enums/`에 위치).

### 2026-05-24
- `JpaAuditingConfig.java`가 로컬에만 있고 git 미추적 상태였음. 해당 파일 없이 dev 브랜치를 실행하면 `BaseTimeEntity.createdAt`/`updatedAt` = null → NOT NULL 위반 → 500. feature/login_minsik → dev 머지로 함께 해소.
- 프론트 담당자가 보고한 POST /api/lessons, POST /api/quiz 500 오류의 실제 원인은 이 JPA 감사 설정 누락이었음 (content null 오류로 오인).
- 프론트 403 메시지 미표시는 프론트엔드에서 `error.response.data.message` 미사용으로 인한 것 — 백엔드 변경 없이 프론트 수정으로 해결.

## 완료 상태

- `feature/login_minsik` → `dev` 머지 완료 (2026-05-24)
- `origin/dev` push 완료
- `./gradlew build` 전체 통과
