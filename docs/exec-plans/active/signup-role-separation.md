# [진행 중] PROF / USER 회원가입 분리

- **시작일**: 2026-05-14
- **브랜치**: feature/signup-role-separation

## 목표

현재 단일 회원가입 엔드포인트(`POST /api/auth/signup`)를 역할별로 분리한다.
- **PROF(교수)** — 로컬 회원가입: `POST /api/auth/profsignup`
- **USER(학생)** — 카카오 소셜 가입 완료: `POST /api/auth/usersignup`

카카오 신규 유저는 즉시 DB에 저장하지 않고, 10분짜리 pending 토큰을 발급해 프론트엔드 정보 입력 페이지로 리다이렉트한다. 유저가 이름·이메일·닉네임을 입력하고 제출하면 그 시점에 DB 저장 + JWT 발급이 이루어진다.

## 수용 기준

- [ ] `POST /api/auth/profsignup` 호출 시 Role.PROF로 유저가 저장된다
- [ ] `POST /api/auth/signup` 엔드포인트는 삭제된다
- [ ] 기존 카카오 유저(DB에 존재)는 로그인 시 기존과 동일하게 JWT가 발급된다
- [ ] 카카오 신규 유저는 로그인 시 DB에 저장되지 않고 pending 토큰과 함께 프론트 가입 페이지로 리다이렉트된다
- [ ] `POST /api/auth/usersignup` 호출 시 pending 토큰 검증 후 Role.USER로 유저가 저장되고 JWT가 발급된다
- [ ] pending 토큰이 만료(10분)되었거나 위·변조된 경우 적절한 에러를 반환한다
- [ ] 이메일·닉네임 중복 시 기존 에러코드(`EMAIL_ALREADY_EXISTS`, `NICKNAME_ALREADY_EXISTS`)를 그대로 반환한다
- [ ] 닉네임 유효성: 영문·숫자·한글, 2~20자 (PROF·USER 공통)
- [ ] `GET /api/auth/check-nickname?nickname=xxx` 호출 시 DB에서 중복 여부를 반환한다
- [ ] `./gradlew test` 통과 (ArchUnit 포함)

## 변경 대상 파일

### 신규 생성
| 파일 | 설명 |
|------|------|
| `domain/user/dto/request/ProfSignupRequestDto.java` | 교수 회원가입 요청 DTO |
| `domain/user/dto/request/UserSocialSignupRequestDto.java` | 학생 소셜 가입 완료 요청 DTO |

### 기존 수정
| 파일 | 변경 내용 |
|------|----------|
| `global/exception/ErrorCode.java` | `INVALID_PENDING_TOKEN` 추가 |
| `global/jwt/JwtProvider.java` | `generatePendingToken()`, `getPendingTokenClaims()` 추가 |
| `global/oauth2/service/CustomOidcUserService.java` | 신규/기존 유저 분기 처리 |
| `global/oauth2/handler/OAuth2SuccessHandler.java` | 신규 유저 → pending 토큰 리다이렉트 분기 추가 |
| `domain/user/service/UserSignupService.java` | `profSignup()`, `findSocialUser()`, `completeSocialSignup()` 추가, `signup()` 제거 |
| `domain/user/service/UserAuthService.java` | `completeSocialSignup()` 추가 (`UserSignupService` 의존성 추가) |
| `domain/user/controller/AuthController.java` | `/profsignup`, `/usersignup`, `GET /check-nickname` 추가, `/signup` 제거 |
| `resources/application-local.yaml` | `app.oauth2.register-uri` 추가 |

### 삭제
| 파일 | 설명 |
|------|------|
| `domain/user/dto/request/SignupRequestDto.java` | `ProfSignupRequestDto`로 대체 |

## 테스트 케이스

### UserSignupServiceTest (8)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `profSignup_success` | 유효한 요청, 이메일·닉네임 미중복 | `Role.PROF` + `AuthProvider.LOCAL`로 저장, 패스워드 인코딩 호출 |
| 2 | `profSignup_passwordMismatch` | `password != passwordConfirm` | `PASSWORD_CONFIRM_MISMATCH`, save 미호출 |
| 3 | `profSignup_emailDuplicate` | 이메일 중복 | `EMAIL_ALREADY_EXISTS`, save 미호출 |
| 4 | `profSignup_nicknameDuplicate` | 닉네임 중복 | `NICKNAME_ALREADY_EXISTS`, save 미호출 |
| 5 | `completeSocialSignup_success` | 이메일·닉네임 미중복 | `Role.USER` + 카카오 provider/providerId로 저장, password=null |
| 6 | `completeSocialSignup_emailDuplicate` | 이메일 중복 | `EMAIL_ALREADY_EXISTS` |
| 7 | `completeSocialSignup_nicknameDuplicate` | 닉네임 중복 | `NICKNAME_ALREADY_EXISTS` |
| 8 | `isNicknameAvailable_returnsTrueWhenNotExists` / `_returnsFalseWhenExists` | 닉네임 존재 여부 | `existsByNickname` 결과의 부정값 반환 |

### JwtProviderTest (5)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `generatePendingToken_containsExpectedClaims` | providerId/provider/name으로 생성 | sub=providerId, type=PENDING_SOCIAL, provider=KAKAO, name 일치 |
| 2 | `getPendingTokenClaims_success` | 유효한 pending 토큰 | providerId/provider/name 맵 반환 |
| 3 | `getPendingTokenClaims_typeMismatch` | access 토큰을 pending으로 파싱 | `INVALID_PENDING_TOKEN` |
| 4 | `getPendingTokenClaims_tampered` | 위·변조된 토큰 | `INVALID_PENDING_TOKEN` |
| 5 | `getPendingTokenClaims_expired` | 만료된 pending 토큰(과거 시각 발급) | `INVALID_PENDING_TOKEN` |

### UserAuthServiceTest 추가 (2)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `completeSocialSignup_success` | 유효 pending 토큰 → UserSignupService 위임, JWT 발급 | accessToken/refreshToken 반환, RefreshToken 저장 |
| 2 | `completeSocialSignup_invalidPendingToken` | JwtProvider가 `INVALID_PENDING_TOKEN` 던짐 | `INVALID_PENDING_TOKEN`, UserSignupService 미호출 |

---

## 의사결정 로그

- **2026-05-14** — pending 토큰 방식 채택 (카카오 신규 유저 즉시 저장 방식 대신)
  - 이유: 가입 완료 전 불필요한 DB 레코드 생성 방지
  - 대안 검토: `PENDING` 상태 컬럼 방식은 불완전 레코드가 DB에 쌓여 관리 복잡도 증가 → 기각
- **2026-05-14** — 리다이렉트 URL에 `kakaoName` 쿼리 파라미터 포함
  - 이유: 프론트엔드에서 이름 필드 pre-fill을 위해 JWT 디코딩 없이 값을 받아야 함
- **2026-05-14** — pending 토큰 유효기간 10분
  - 이유: 가입 완료에 충분한 시간이면서, 탈취 시 피해를 최소화하는 균형점
- **2026-05-14** — PROF 이메일 인증 API는 이번 작업에서 제외
  - 이유: 다른 팀원이 별도 개발 예정
- **2026-05-14** — `UserSocialSignupRequestDto`에 `username` 포함 (카카오 이름 기본값, 수정 가능)
  - 이유: 카카오 이메일 스코프 미지원, 이름도 사용자가 직접 입력·수정해야 함
- **2026-05-14** — `JwtProvider.getPendingTokenClaims()` catch 절 수정 (`CustomException` → `JwtException | IllegalArgumentException`)
  - 이유: 단위 테스트에서 발견. `parseClaims()`는 jjwt 예외를 직접 던지므로 `CustomException`을 잡던 기존 코드는 만료/위변조 시 raw JJWT 예외가 그대로 빠져나가 `INVALID_PENDING_TOKEN`으로 변환되지 않았음
- **2026-05-14** — `application-local.yaml`의 JWT secret을 32바이트 키로 교체
  - 이유: 기존 키가 31바이트(248비트)라 jjwt 0.12.x의 HMAC-SHA 최소 요구치(256비트)에 미달하여 `WeakKeyException`으로 Spring Context 로딩 실패. 회원가입 분리 작업과 직접 관련은 없으나 테스트 통과 블로커였음
