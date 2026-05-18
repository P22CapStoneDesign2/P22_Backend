# EQH Backend

교수(강사)와 학생을 위한 **교안 관리 및 퀴즈(문제 은행) 플랫폼**의 백엔드입니다.

- **언어 / 프레임워크**: Java 21, Spring Boot 3.5.x, Gradle (Kotlin DSL)
- **패키지 루트**: `com.capstone.eqh`
- **DB / 캐시**: PostgreSQL, Redis (이메일 인증 코드 저장)
- **인증**: JWT (Access 30분 / Refresh 7일, Rotation 적용) + Kakao OAuth2 (OIDC)

---

## 빠른 시작

```bash
./gradlew build       # 빌드 (ArchUnit 아키텍처 테스트 포함)
./gradlew bootRun     # 애플리케이션 실행
./gradlew test        # 전체 테스트 실행
```

### 사전 요구사항

- JDK 21
- PostgreSQL (로컬 또는 원격)
- Redis (이메일 인증 기능 사용 시 필수)
- SMTP 서버 / 계정 (메일 발송용)
- `application.yml` 또는 환경 변수로 DB / Redis / 카카오 OAuth2 / 메일 설정 주입

---

## 주요 기능

| 도메인 | 경로 | 역할 |
|--------|------|------|
| `user` | `/api/auth/**`, `/api/users/**` | 로그인, 회원가입(PROF/USER 분리), 이메일 인증, 카카오 소셜 로그인, 프로필 관리 |
| `quiz` | `/api/quiz/**` | 퀴즈 세트·문제 CRUD, 자동 채점, 오답 정리 |
| `lesson` | `/api/lessons/**`, `/api/admin/lessons/**` | 교안 뷰어, 관리자 교안 관리 |

### 권한 (RBAC)

- `PROF` — 강사 (교안·퀴즈 생성/수정)
- `USER` — 학생 (교안 열람, 퀴즈 응시, 오답 조회)
- `ADMIN` — 서비스 관리자

자세한 권한별 시나리오는 [docs/PRD.md](docs/PRD.md) 참고.

---

## 프로젝트 구조

```
src/main/java/com/capstone/eqh/
├── eqhApplication.java
├── domain/
│   ├── user/      # 인증, 회원가입, 이메일 인증, 프로필
│   ├── quiz/      # 퀴즈 세트·문제·제출·채점·오답
│   └── lesson/    # 교안
└── global/
    ├── jwt/       # JWT 발급·검증·필터
    ├── oauth2/    # 카카오 OIDC
    ├── security/  # FilterChain, RBAC, PasswordEncoder
    ├── config/    # Redis, EmailVerificationProperties
    ├── exception/ # 공통 예외 처리
    ├── common/    # ApiResponse, BaseTimeEntity
    └── util/      # CryptoHashUtil 등
```

상세 패키지 구조와 의존 규칙은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 참고.

---

## 코딩 컨벤션 (요약)

1. **DTO 네이밍**: `...RequestDto` / `...ResponseDto`, 위치는 `dto/request/`, `dto/response/`
2. **공통 응답**: 모든 API는 `ApiResponse<T>` 래퍼로 응답
3. **예외 처리**: `CustomException` + `ErrorCode` enum + `GlobalExceptionHandler`
4. **응답 메시지**: 항상 한국어
5. **소유자 검증 메서드 시그니처**: `isOwner(Long id, Long userId)` — `CustomUserDetails` 파라미터 사용 금지
6. **레이어 의존 규칙**: `..domain..` 패키지는 `global.jwt`, `global.oauth2`, `global.security`에 의존 금지 (ArchUnit으로 강제, 위반 시 `./gradlew test` 실패)
   - 예외: `domain.user.service`(JWT 직접 사용 인증 서비스), `..domain..controller`(`@AuthenticationPrincipal` 사용)

---

## 문서

| 문서 | 내용 |
|------|------|
| [docs/PRD.md](docs/PRD.md) | 서비스 기획, 권한 정의, 시나리오 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 패키지 구조, 레이어 의존 규칙 |
| [docs/API.md](docs/API.md) | 전체 API 명세 |
| [docs/QUALITY.md](docs/QUALITY.md) | 도메인별 품질 등급 및 개선 우선순위 |
| [docs/SECURITY.md](docs/SECURITY.md) | 보안 체크리스트 |
| [docs/design-docs/](docs/design-docs/) | 설계 결정 기록 (왜 이렇게 만들었는가) |
| [docs/exec-plans/](docs/exec-plans/) | 기능 구현 계획 (active / completed) |
| [docs/troubleshooting/](docs/troubleshooting/) | 실제 발생 이슈와 해결 과정 |
| [docs/generated/db-schema.md](docs/generated/db-schema.md) | 엔티티 기반 DB 스키마 |
| [docs/EQH.postman_collection.json](docs/EQH.postman_collection.json) | Postman 컬렉션 |

---

## 테스트

- **단위 테스트**: JUnit 5 + Mockito (서비스 레이어 필수)
- **아키텍처 테스트**: ArchUnit — `src/test/java/com/capstone/eqh/ArchitectureTest.java`
- **JWT 필터/프로바이더 테스트**: `src/test/java/com/capstone/eqh/global/jwt/`

```bash
./gradlew test                                              # 전체
./gradlew test --tests "com.capstone.eqh.ArchitectureTest"  # 특정 테스트
```

---

## 브랜치 전략

- `main` — 배포 브랜치
- `dev` — 통합 개발 브랜치 (기능 머지 대상)
- `feat/*` — 기능 개발 브랜치
