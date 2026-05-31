# [완료] JwtFilter — public 경로 토큰 검증 skip

- **시작일**: 2026-05-14
- **완료일**: 2026-05-14
- **브랜치**: feature/jwt-filter-public-skip
- **연관 작업**: `completed/signup-role-separation.md` (이번 작업으로 노출된 UX 버그)

## 목표

`JwtFilter`가 **모든** 요청에 대해 Authorization 헤더의 토큰을 검증하기 때문에,
SecurityConfig에서 `permitAll()`로 열어둔 공개 엔드포인트(`/api/auth/**` 등)도
헤더에 무효/만료된 토큰이 실려 오면 401로 차단된다.

회원가입·로그인·재발급은 정의상 인증되지 않은 상태에서 호출되는 익명 API이므로,
이런 경로에서는 JwtFilter가 토큰 검증을 시도조차 하지 않도록 한다.

## 배경 (실제 발생한 버그)

- `signup-role-separation` 작업에서 `application-local.yaml`의 JWT secret을 교체함
- 브라우저에 남아 있던 access token은 옛 키로 서명되어 있어 `INVALID_TOKEN` 처리됨
- 사용자가 회원가입 폼을 제출하자 axios가 Authorization 헤더에 옛 토큰을 자동 첨부 →
  JwtFilter가 컨트롤러 도달 전에 401 "유효하지 않은 토큰입니다." 반환 →
  새 사용자가 가입조차 못 함

같은 증상은 access token이 만료된 채로 회원가입·재발급·로그아웃을 시도할 때도 재현된다.

## 수용 기준

- [x] `/api/auth/**`, `/oauth2/**`, `/login/oauth2/**` 경로는 Authorization 헤더의 토큰 유효성과 무관하게 컨트롤러까지 통과한다
- [x] 무효 토큰이 실려와도 위 경로는 401로 차단되지 않는다 (회원가입·로그인·재발급·로그아웃이 토큰 상태와 독립적으로 동작)
- [x] 보호된 경로(`/api/users/**`, `/api/lessons/**`, `/api/quiz/**`, `/api/admin/**`)는 기존과 동일하게 토큰 검증 + 401/403 처리
- [x] `./gradlew test` 통과 — `JwtFilterTest` 4건 신규 통과, 기타 53건 모두 통과 (`contextLoads()`는 Supabase DB 일시 연결 불가로 환경 이슈, 본 변경과 무관)

## 변경 대상 파일

### 기존 수정
| 파일 | 변경 내용 |
|------|----------|
| `global/jwt/JwtFilter.java` | `shouldNotFilter()` 오버라이드 추가 — public 경로 prefix 매칭 시 필터 자체를 skip |

### 신규 생성
| 파일 | 변경 내용 |
|------|----------|
| `test/.../global/jwt/JwtFilterTest.java` *(또는 통합 테스트)* | public 경로에 무효 토큰을 보내도 401이 아닌 컨트롤러 응답이 나오는지 검증 |

### 문서 갱신 (4단계)
- `docs/SECURITY.md` — JwtFilter 동작 명세 항목 추가
- `docs/troubleshooting/jwt-filter-public-401.md` — 이번에 발견된 증상·원인·조치 기록
- `docs/design-docs/auth-flow.md` *(있으면)* — public 경로 skip 명시

## 테스트 케이스

### JwtFilterTest (3)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `skipsValidation_onAuthPath` | `/api/auth/profsignup` + 위변조 토큰 헤더 | 필터 통과, 컨트롤러로 진입 (예: 400/422가 나와도 401은 아님) |
| 2 | `skipsValidation_onOAuth2Path` | `/oauth2/authorization/kakao` + 만료 토큰 헤더 | 필터 통과 |
| 3 | `validatesToken_onProtectedPath` | `/api/users/me` + 위변조 토큰 헤더 | 401 `INVALID_TOKEN` |

> 단위 테스트는 `MockHttpServletRequest`/`MockFilterChain`로 작성 가능 — Spring 컨텍스트 불필요.
> 통합 테스트로 갈 경우 `@WebMvcTest` + `@Import(JwtFilter.class)`.

## 의사결정 로그

- **2026-05-14** — `shouldNotFilter()` 방식 채택
  - 이유: `OncePerRequestFilter`의 표준 확장점. 한 곳에서 화이트리스트를 명시적으로 선언 → 가독성·테스트 용이
  - 대안 검토: `doFilterInternal()` 안에서 path 검사하는 방식은 조건이 분산되어 추후 누락 위험 → 기각
- **2026-05-14** — skip 대상에 `/api/auth/logout`도 포함
  - 이유: 현재 `UserAuthService.logout()`은 access token이 아닌 request body의 refresh token으로 동작함 → access token 유효성과 무관
  - 단, API.md에는 "🔒 인증 필요"로 표기되어 있어 명세-동작 불일치가 있음. 본 작업에서는 동작에 맞춰 skip 처리하고, API.md 표기를 함께 정정
- **2026-05-14** — skip 대상에 `/api/auth/reissue` 포함
  - 이유: reissue는 정의상 access token이 만료된 상태에서 호출되는 엔드포인트. 헤더의 access token이 expired/invalid라고 401로 막으면 reissue가 영구히 불가능해짐
