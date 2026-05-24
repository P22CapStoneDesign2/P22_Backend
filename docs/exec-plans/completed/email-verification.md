# [Completed] 이메일 인증 (PROF 회원가입)

- **시작일**: 2026-05-17
- **완료일**: 2026-05-24
- **브랜치**: feat/emailchek → dev 머지
- **관련 결정**: [design-docs/email-verification.md](../../design-docs/email-verification.md)

## 목표

PROF 로컬 회원가입(`POST /api/auth/profsignup`) 전 단계에 이메일 소유 확인을 강제한다.
- 사용자가 입력한 이메일로 6자리 인증번호를 발송하고, 5분 안에 동일 코드를 제출하면 인증 완료 상태를 30분 유지한다.
- 회원가입은 인증 완료 상태에서만 진행된다.
- 본 브랜치는 별도 팀원(`origin/feature/EmailCheck_jihun`)이 dev와 무관한 노베이스에서 만든 이메일 검증 코드를 dev 컨벤션에 맞춰 이식한 결과다.

## 수용 기준

- [x] `POST /api/auth/email/send` — 인증번호 발송, 200 + 성공 메시지
- [x] `POST /api/auth/email/verify` — 코드 일치 시 200, Redis `ev:verified:{email}`에 30분 TTL 플래그 저장
- [x] 인증 완료되지 않은 이메일로 `/api/auth/profsignup` 호출 시 `EMAIL_NOT_VERIFIED`(403)
- [x] 인증번호 재발송 쿨다운 60초 → `VERIFICATION_SEND_COOLDOWN`(429)
- [x] 1시간 윈도우 내 5회 초과 발송 시 `VERIFICATION_SEND_LIMIT_EXCEEDED`(429)
- [x] 인증번호 5회 불일치 시 `VERIFICATION_ATTEMPTS_EXCEEDED`(423), 이후 15분간 `VERIFICATION_LOCKED`(423)
- [x] 인증번호 TTL(5분) 경과 시 `VERIFICATION_NOT_FOUND_OR_EXPIRED`(404)
- [x] Redis에는 평문 인증번호가 아닌 HMAC-SHA256 해시만 저장됨
- [x] `./gradlew test` 통과 (ArchUnit 포함, 기존 회원가입 테스트는 이메일 인증 통합 후에도 통과)

## 변경 대상 파일

### 신규 생성
| 파일 | 설명 |
|------|------|
| `domain/user/controller/EmailVerificationController.java` | `/api/auth/email/send`, `/verify` REST 컨트롤러 |
| `domain/user/dto/request/EmailSendRequestDto.java` | 발송 요청 (email) |
| `domain/user/dto/request/EmailVerifyRequestDto.java` | 검증 요청 (email, code) |
| `domain/user/service/EmailVerificationService.java` | 발송·검증 유스케이스 |
| `domain/user/service/MailNotificationService.java` | SMTP 발송 어댑터 (JavaMailSender) |
| `domain/user/redis/EmailVerificationRedisStore.java` | Redis 키 접근(코드/쿨다운/카운터/잠금/완료) |
| `global/config/EmailVerificationProperties.java` | `app.email-verification.*` ConfigurationProperties |
| `global/config/RedisConfig.java` | `StringRedisTemplate` Bean |
| `global/util/CryptoHashUtil.java` | HMAC-SHA256 hex 헬퍼 |
| `docs/email-verification-testing.md` | jihun 작성 수동 테스트 가이드 (그대로 복사) |
| `docs/design-docs/email-verification.md` | 설계 결정 (Redis 키 / HMAC / 쿨다운·잠금 정책) |

### 기존 수정
| 파일 | 변경 내용 |
|------|----------|
| `global/exception/ErrorCode.java` | `EMAIL_NOT_VERIFIED`, `VERIFICATION_NOT_FOUND_OR_EXPIRED`, `VERIFICATION_LOCKED`, `VERIFICATION_ATTEMPTS_EXCEEDED`, `VERIFICATION_SEND_COOLDOWN`, `VERIFICATION_SEND_LIMIT_EXCEEDED`, `VERIFICATION_CODE_MISMATCH` 추가 |
| `domain/user/service/UserSignupService.java` | `profSignup()` 진입부에 `emailVerificationService.requireEmailVerifiedForSignup()` 호출, 성공 후 `consumeEmailVerification()` 호출 |
| `eqhApplication.java` | `@ConfigurationPropertiesScan` 추가 (`EmailVerificationProperties` 등록용) |
| `build.gradle.kts` | `spring-boot-starter-data-redis`, `spring-boot-starter-mail` 의존성 추가 |
| `resources/application-local.yaml` | `spring.data.redis.*`, `spring.mail.*`, `app.email-verification.*` 섹션 추가 |
| `docs/API.md` | `/api/auth/email/send`, `/verify` 엔드포인트 추가 + profsignup Response에 `EMAIL_NOT_VERIFIED` 명시 |
| `docs/design-docs/index.md` | `email-verification.md` 항목 추가 |

### 가져오지 않은 파일 (jihun 브랜치 → 제외 사유)
| 파일 | 제외 사유 |
|------|----------|
| `com/capstone/backend/CapstoneBackendApplication.java` | dev에 `eqhApplication`이 이미 존재 |
| `domain/auth/entity/User.java`, `domain/auth/repository/UserRepository.java` | dev `domain/user`의 것을 사용 |
| `domain/auth/service/SignupUserService.java` | dev `UserSignupService`로 통합 |
| `global/config/SecurityConfig.java` | dev `SecurityConfig`는 이미 `/api/auth/**` permitAll |
| `global/response/ApiResponse.java`, `global/exception/ErrorBody.java`, `global/exception/GlobalExceptionHandler.java` | dev `common/ApiResponse` + `exception/CustomException`·`GlobalExceptionHandler` 사용 |
| `domain/auth/dto/response/EmailVerifyDataResponse.java` | 컨트롤러가 사용하지 않음 |
| `domain/auth/exception/*.java` (8개) | dev `ErrorCode` enum + `CustomException` 단일 체계로 통합 |
| `src/main/resources/application.yml` | dev는 `application.yaml`(local profile) 사용 — 필요한 키만 `application-local.yaml`에 머지 |
| `.vscode/settings.json`, `bin/.gitkeep`, `docs/.gitkeep`, `reference/.gitkeep` | 환경/잡 파일 |

## 의사결정 로그

- **2026-05-17** — `feature/EmailCheck_jihun`을 그대로 머지하지 않고 dev 컨벤션에 맞춰 재이식
  - 이유: jihun 브랜치는 dev와 공통 조상이 없는 노베이스. 패키지 루트(`com.capstone.backend`)와 ApiResponse·예외 체계가 dev(`com.capstone.eqh`)와 충돌. 그대로 머지하면 dev 도메인이 모두 삭제됨
  - 대안 검토: `--allow-unrelated-histories` 머지 → 패키지 충돌과 ArchUnit 위반으로 빌드 실패 예상. cherry-pick → 단일 커밋이라 이점 없음
- **2026-05-17** — 동적 메시지(`"남은 시도: N회"`, `"약 N초"`)는 ErrorCode 정적 메시지로 대체
  - 이유: dev `CustomException(ErrorCode)`은 정적 메시지만 받음. 동적 값을 message에 끼우려면 `CustomException` 시그니처 확장 필요 → 본 브랜치 범위에서 제외
  - 영향: 사용자 안내가 다소 막연("잠시 후 다시 시도")해지지만 응답 일관성을 우선
- **2026-05-17** — `domain/user` 패키지 안에 이메일 인증 위치
  - 이유: jihun 원본은 `domain/auth`였으나 dev는 `domain/user`로 인증 도메인을 통일하고 있음
- **2026-05-17** — `SignupUserService` 별도 가져오지 않고 dev `UserSignupService.profSignup()`에 인증 검증 통합
  - 이유: 가입 책임 단일 진입점 유지. PROF 가입 시점에만 강제하고, 카카오 USER는 카카오 인증으로 이메일 소유 검증을 대체
- **2026-05-17** — Mail 발송: SMTP 활성화 (jihun 원본은 mock 콘솔 출력)
  - 이유: 사용자 결정. `MAIL_USERNAME`/`MAIL_PASSWORD` 환경변수 미설정 시 발송 단계에서 예외
- **2026-05-17** — `EmailVerificationProperties`는 `@ConfigurationPropertiesScan`으로 등록
  - 이유: Spring Boot 3.x 권장 패턴. 향후 다른 properties 추가 시 별도 설정 불필요

## 완료 상태

- feat/emailchek → dev 머지 완료
- `./gradlew build` 전체 통과
- 단위 테스트(`EmailVerificationServiceTest`)는 향후 추가 예정
