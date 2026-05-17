# 이메일 인증 설계 결정

> 상태: ✅ 확정 | 대상: `domain/user/` (controller/service/redis), `global/config/`, `global/util/`

---

## 결정 1 — PROF 회원가입에 한해 이메일 인증 강제

**결정**: `/api/auth/profsignup` 호출 전에 동일 이메일로 인증 완료 상태(`ev:verified:{email}`)가 있어야 한다.

**이유**:
- 학생(USER)은 카카오 OIDC 동의 화면을 통과하므로 이메일 소유 증명이 별도로 필요 없음
- 교수(PROF)는 자유 입력 이메일로 가입하므로 타인 이메일 도용 방지가 필요

**구현**: `UserSignupService.profSignup()` 진입부에서 `EmailVerificationService.requireEmailVerifiedForSignup()` 호출. 성공 후 `consumeEmailVerification()`으로 플래그 제거(재가입 방지).

---

## 결정 2 — Redis 기반 상태 저장 (DB 컬럼 추가 없음)

**결정**: 인증번호·쿨다운·실패 카운터·잠금·인증 완료 모두 Redis TTL 키로 관리. `users` 테이블에는 컬럼 추가하지 않는다.

**이유**:
- 자동 만료(TTL)로 정리 비용 0
- 인증번호 같은 단명 상태를 영구 저장소(Postgres)에 두지 않음
- DB 스키마 마이그레이션 불필요

**트레이드오프**: Redis 장애 시 인증 전체 불가. 운영에서는 Redis HA 또는 별도 fallback 필요(이번 브랜치 범위 밖).

### Redis 키 스키마

정규화된 이메일(`trim().toLowerCase`)을 suffix로 사용.

| 키 | 값 | TTL | 용도 |
|----|----|-----|------|
| `ev:code:{email}` | `{failedAttempts}:{codeHash}` | 5분 (`code-ttl-seconds`) | 발송된 인증번호의 HMAC 해시 + 누적 실패 횟수 |
| `ev:cooldown:{email}` | `1` | 60초 (`resend-cooldown-seconds`) | 재발송 쿨다운 플래그 |
| `ev:send:{email}` | 카운터(증가) | 1시간 (`send-window-seconds`) | 시간 윈도우 내 발송 횟수 |
| `ev:lock:{email}` | `1` | 15분 (`lock-duration-seconds`) | 시도 초과 잠금 |
| `ev:verified:{email}` | `1` | 30분 (`verified-ttl-seconds`) | 인증 완료 — 회원가입 허용 윈도우 |

`ev:code:{email}` 값의 `{failedAttempts}:{codeHash}` 직렬화는 `EmailVerificationRedisStore.serializeCodeState`/`deserialize` 한 곳에서만 처리한다.

---

## 결정 3 — 인증번호는 평문이 아닌 HMAC-SHA256 해시로만 Redis에 보관

**결정**: 6자리 코드 자체는 메일 발신 직후 메모리에서 사라지고, Redis에는 `HMAC-SHA256(secret, email + "|" + code)`만 저장.

**이유**:
- Redis 덤프나 권한 오설정으로 키가 유출되어도 인증번호가 노출되지 않음
- HMAC secret은 환경변수(`EMAIL_VERIFY_HMAC_SECRET`)로 분리

**구현**: `CryptoHashUtil.hmacSha256Hex(secret, payload)`. secret이 약하면 동등한 평문 저장과 다름없으므로 운영에서는 반드시 `app.email-verification.hmac-secret`을 강한 값으로 덮어쓴다.

---

## 결정 4 — 쿨다운·발송 한도·잠금의 다층 방어

**결정**: 발송·검증 양쪽에 별도 한도를 둔다.

| 정책 | 값 | 키 | 위반 시 ErrorCode |
|------|----|----|------------------|
| 동일 이메일 재발송 최소 간격 | 60초 | `ev:cooldown` | `VERIFICATION_SEND_COOLDOWN` (429) |
| 1시간 윈도우 발송 횟수 | 5회 | `ev:send` | `VERIFICATION_SEND_LIMIT_EXCEEDED` (429) |
| 인증번호 검증 시도 횟수 | 5회 | `ev:code`(failedAttempts) | `VERIFICATION_CODE_MISMATCH` (401) (5회째 누적 시 423) |
| 시도 초과 시 잠금 | 15분 | `ev:lock` | `VERIFICATION_LOCKED` (423) |

**이유**: 무차별 대입(브루트포스)과 SMS/메일 폭격을 함께 막아야 함. 발송 측 한도(쿨다운+윈도우 카운터)와 검증 측 한도(시도 횟수+잠금)는 직교한다.

**HTTP 상태 매핑**: 429(쿨다운/발송 한도), 423(잠금), 401(코드 불일치), 404(코드 만료/없음).

---

## 결정 5 — 회원가입 성공 시 인증 완료 플래그 즉시 폐기

**결정**: `UserSignupService.profSignup()` 성공 후 `EmailVerificationService.consumeEmailVerification()` 호출하여 `ev:verified:{email}` 삭제.

**이유**:
- 동일 인증 완료 상태로 여러 번 가입 시도(이론상 이메일은 unique지만 race 조건 방지)
- 30분 윈도우가 자동 만료되기 전에도 1회성으로 소비

---

## 결정 6 — 인증 완료 윈도우 30분

**결정**: 인증 완료 후 회원가입까지 허용되는 시간은 30분(`verified-ttl-seconds=1800`).

**이유**: 검증 시점과 회원가입 폼 제출 시점이 분리되어도 자연스러운 가입 흐름이 가능한 길이. 너무 짧으면 폼 작성 중 만료, 너무 길면 인증 의미가 약해짐.

**대안 검토**: 5분 → 폼 작성 시간 부족 우려. 24시간 → 사실상 인증 효과 없음.

---

## 결정 7 — 동적 안내 메시지 미적용 (재시도 N초, 남은 시도 M회)

**결정**: ErrorCode의 정적 메시지를 그대로 사용한다. "약 N초 후" 같은 동적 값은 응답 본문에 포함하지 않는다.

**이유**: dev `CustomException`은 `ErrorCode`만 받고 message를 오버라이드하지 않는 단일 진입 체계. 동적 메시지를 위해 `CustomException` 시그니처를 확장하면 전 도메인 응답 형식이 바뀜 → 본 브랜치 범위 초과.

**향후 개선 후보**: `CustomException` 또는 `ApiResponse`에 `retryAfterSeconds`·`remainingAttempts` 필드 추가 → tech-debt-tracker로 분리.
