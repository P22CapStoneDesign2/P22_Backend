# 이메일 인증 API — Postman·Redis 테스트

## Redis 키 요약

| 논리 | 실제 키 패턴 | 값 | TTL |
|------|----------------|-----|-----|
| 코드+실패횟수 | `ev:code:{정규화이메일}` | `{failedAttempts}:{hmacHex}` | `app.email-verification.code-ttl-seconds` (기본 300초) |
| 재발송 쿨다운 | `ev:cooldown:{email}` | `1` | `resend-cooldown-seconds` |
| 발송 횟수 | `ev:send:{email}` | 숫자 문자열 | `send-window-seconds` |
| 잠금 | `ev:lock:{email}` | `1` | `lock-duration-seconds` |
| 인증 완료 | `ev:verified:{email}` | `1` | `verified-ttl-seconds` |

이메일은 항상 **trim + lower case** 한 값이 suffix에 들어간다.

---

## Windows + Docker Redis 디버깅

컨테이너 이름이 `redis` 라고 가정한다.

### 컨테이너 셸에서 redis-cli

```powershell
docker exec -it redis redis-cli
```

### 키 목록 (패턴)

```redis
KEYS ev:*
```

운영/대용량에서는 `SCAN` 사용을 권장한다.

```redis
SCAN 0 MATCH ev:* COUNT 100
```

### 특정 이메일 코드 상태

```redis
GET ev:code:test@example.com
TTL ev:code:test@example.com
```

### verified / lock

```redis
GET ev:verified:test@example.com
TTL ev:verified:test@example.com
GET ev:lock:test@example.com
TTL ev:lock:test@example.com
```

### 쿨다운·발송 카운트

```redis
GET ev:cooldown:test@example.com
TTL ev:cooldown:test@example.com
GET ev:send:test@example.com
TTL ev:send:test@example.com
```

### 키 삭제(초기화)

```redis
DEL ev:code:test@example.com ev:verified:test@example.com ev:lock:test@example.com ev:cooldown:test@example.com ev:send:test@example.com
```

---

## Postman 시나리오 (순서)

1. **인증번호 발송**  
   `POST http://localhost:8080/api/auth/email/send`  
   Body: `{ "email": "test@example.com" }`  
   → `success: true`, 콘솔/목에 6자리 코드 확인.

2. **Redis 코드 키 확인**  
   `GET ev:code:test@example.com` — 값은 `0:{해시}` 형태.

3. **잘못된 코드**  
   `POST http://localhost:8080/api/auth/email/verify`  
   Body: `{ "email": "test@example.com", "code": "000000" }`  
   → `401`, `errorCode`: `VERIFICATION_CODE_MISMATCH`, 메시지에 남은 시도.

4. **failedAttempts 증가 확인**  
   `GET ev:code:test@example.com` — 앞자리 숫자가 `1`, `2`, … 로 증가하는지 확인.

5. **올바른 코드**  
   발송 시 출력된 6자리로 `verify` 호출  
   → `200`, `success: true`, 메시지 "이메일 인증이 완료되었습니다."

6. **verified 확인**  
   `GET ev:verified:test@example.com` → `1`, TTL 존재.  
   `GET ev:code:test@example.com` → `(nil)` (성공 시 코드 키 삭제).

7. **회원가입(예시 서비스)**  
   `SignupUserService.registerEmailAfterVerification("test@example.com")` 호출 시  
   verified 없으면 `403` + `EMAIL_NOT_VERIFIED`.  
   성공 시 DB에 사용자 저장 후 `ev:verified:` 키 삭제.

---

## 응답 형식

**성공**

```json
{
  "success": true,
  "message": "이메일 인증이 완료되었습니다."
}
```

**실패**

```json
{
  "success": false,
  "message": "…",
  "errorCode": "VERIFICATION_CODE_MISMATCH"
}
```

`data` 필드는 null이라 JSON에서 생략될 수 있다.

---

## verify 관련 errorCode

| 상황 | HTTP | errorCode |
|------|------|-----------|
| 코드 불일치(한도 내) | 401 | `VERIFICATION_CODE_MISMATCH` |
| 코드 없음/만료 | 404 | `VERIFICATION_NOT_FOUND_OR_EXPIRED` |
| 잠금(ev:lock) | 423 | `VERIFICATION_LOCKED` |
| 최대 실패로 잠금 처리 직후 | 423 | `VERIFICATION_ATTEMPTS_EXCEEDED` |
