# EQH Backend

교수(강사)와 학생을 위한 **교안 관리 및 퀴즈(문제 은행) 플랫폼**의 백엔드입니다.

| 항목 | 내용 |
|------|------|
| 언어 / 프레임워크 | Java 21, Spring Boot 3.5.x |
| 빌드 | Gradle (Kotlin DSL) |
| DB | PostgreSQL (Supabase) |
| 캐시 | Redis (이메일 인증 코드) |
| 인증 | JWT (Access 30분 / Refresh 7일, Rotation) + Kakao OAuth2 (OIDC) |
| 패키지 루트 | `com.capstone.eqh` |

---

## 빠른 시작

```bash
./gradlew build       # 전체 빌드 (ArchUnit 아키텍처 테스트 포함)
./gradlew bootRun     # 애플리케이션 실행
./gradlew test        # 테스트만 실행
```

### 사전 요구사항

- JDK 21
- PostgreSQL (또는 Supabase 원격 DB)
- Redis (이메일 인증 기능에 필수)
- Gmail SMTP 계정 (또는 다른 SMTP 서버)

---

## 주요 기능

### 사용자 역할 (RBAC)

| Role | 의미 | 가입 방식 |
|------|------|----------|
| `PROF` | 강사 — 교안·퀴즈 생성, 수강 신청 관리 | 로컬 회원가입 → ADMIN 승인 후 활성화 |
| `USER` | 학생 — 교안 열람, 퀴즈 응시, 오답 조회 | Kakao 소셜 로그인 |
| `ADMIN` | 서비스 관리자 — PROF 승인/거절, 전체 관리 | DB 직접 삽입 |

### PROF 승인 흐름

1. PROF가 `/api/auth/email/send` → `/api/auth/email/verify` → `/api/auth/profsignup` 순서로 가입
2. 가입 직후 `status = PENDING` — 토큰은 발급되지만 PROF 전용 API는 403 차단
3. ADMIN이 `/api/admin/users/{id}/approve` 호출 → `status = ACTIVE` → 모든 기능 사용 가능
4. `GET /api/users/me` 응답의 `status` 필드로 현재 상태 확인 가능

### API 엔드포인트 요약

| 도메인 | 주요 경로 |
|--------|----------|
| 인증 | `POST /api/auth/profsignup`, `POST /api/auth/login`, `GET /oauth2/authorization/kakao`, `POST /api/auth/reissue` |
| 이메일 인증 | `POST /api/auth/email/send`, `POST /api/auth/email/verify` |
| 사용자 | `GET /api/users/me`, `PATCH /api/users/me`, `DELETE /api/users/me` |
| 교안 | `POST /api/lessons`, `GET /api/lessons`, `GET /api/lessons/{id}`, `PUT`, `DELETE` |
| 수강 신청 | `POST /api/lessons/{id}/enrollments`, `GET`, `approve`, `reject` |
| 퀴즈 | `POST /api/quiz`, `GET /api/quiz`, `GET /api/quiz/{quizId}`, `PUT`, `DELETE` |
| 퀴즈 문제 | `POST /api/quiz/{quizId}/questions`, `PUT`, `DELETE` |
| 퀴즈 제출 | `POST /api/quiz/{quizId}/submit`, `GET /api/quiz/wrong-answers` |
| 관리자 | `GET /api/admin/users/pending`, `approve`, `reject`, `PATCH /api/admin/users/{id}/status` |

전체 명세는 [docs/API.md](docs/API.md) 참고.

---

## 프로젝트 구조

```
src/main/java/com/capstone/eqh/
├── eqhApplication.java
├── domain/
│   ├── user/       # 인증·회원가입·이메일 인증·프로필·PROF 승인
│   ├── lesson/     # 교안·수강 신청
│   └── quiz/       # 퀴즈 세트·문제·제출·채점·오답
└── global/
    ├── jwt/        # JWT 발급·검증·필터
    ├── oauth2/     # Kakao OIDC
    ├── security/   # FilterChain, RBAC, PasswordEncoder
    ├── config/     # Redis, JPA Auditing, EmailVerificationProperties
    ├── exception/  # CustomException, ErrorCode, GlobalExceptionHandler
    ├── common/     # ApiResponse, BaseTimeEntity
    └── util/       # CryptoHashUtil
```

상세 패키지 구조와 의존 규칙은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 참고.

---

## 코딩 컨벤션 (요약)

- **공통 응답**: 모든 API는 `ApiResponse<T>` 래퍼 사용
- **예외 처리**: `CustomException` + `ErrorCode` enum + `GlobalExceptionHandler`
- **응답 메시지**: 한국어로 작성
- **DTO 네이밍**: `...RequestDto` / `...ResponseDto`, 위치는 `dto/request/`, `dto/response/`
- **레이어 의존 규칙**: `..domain..`은 `global.jwt`, `global.oauth2`, `global.security`에 의존 금지 — ArchUnit으로 강제 (`./gradlew test` 실패로 확인)
  - 예외: `domain.user.service`(인증 서비스), `..domain..controller`(`@AuthenticationPrincipal`)
- **소유자 검증**: `isOwner(Long id, Long userId)` 형태 — `CustomUserDetails` 직접 전달 금지

---

## 테스트

- **단위 테스트**: JUnit 5 + Mockito (서비스 레이어)
- **아키텍처 테스트**: ArchUnit — `ArchitectureTest.java`
- **JWT 테스트**: `global/jwt/` 하위

```bash
./gradlew test                                              # 전체
./gradlew test --tests "com.capstone.eqh.ArchitectureTest"  # ArchUnit만
```

---

## 문서

| 문서 | 내용 |
|------|------|
| [docs/API.md](docs/API.md) | 전체 REST API 명세 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 패키지 구조, 레이어 의존 규칙 |
| [docs/PRD.md](docs/PRD.md) | 서비스 기획, 권한 정의, 사용자 시나리오 |
| [docs/SECURITY.md](docs/SECURITY.md) | 인증·인가·보안 체크리스트 |
| [docs/QUALITY.md](docs/QUALITY.md) | 도메인별 품질 등급 및 개선 우선순위 |
| [docs/generated/db-schema.md](docs/generated/db-schema.md) | 엔티티 기반 DB 스키마 |
| [docs/design-docs/](docs/design-docs/) | 설계 결정 기록 |
| [docs/exec-plans/](docs/exec-plans/) | 기능 구현 계획 (active / completed) |
| [docs/troubleshooting/](docs/troubleshooting/) | 실제 발생 이슈와 해결 과정 |
| [docs/EQH.postman_collection.json](docs/EQH.postman_collection.json) | Postman 컬렉션 |

---

## 브랜치 전략

- `main` — 배포 브랜치
- `dev` — 통합 개발 브랜치 (기능 머지 대상)
- `feat/*` — 기능 개발 브랜치
