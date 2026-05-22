# 비밀번호 재설정 API 명세서

## 개요

사용자가 "비밀번호 찾기" 버튼을 클릭하면 이메일로 비밀번호 재설정 링크를 발송하고,  
사용자는 해당 링크를 통해 새 비밀번호를 설정할 수 있다.

---

# 1. 비밀번호 재설정 메일 요청 API

사용자가 이메일 입력 후 호출하는 API

## Request

- Method: `POST`
- URL: `/api/v1/auth/password/reset-request`

### Request Body

```json
{
  "email": "test@example.com"
}
```

---

## Validation

| 필드 | 조건 |
|---|---|
| email | 필수 |
| email | 이메일 형식 |
| email | 가입된 계정 여부 확인 |

---

## Success Response

```json
{
  "success": true,
  "message": "비밀번호 재설정 메일이 발송되었습니다."
}
```

---

## Fail Response

### 이메일 형식 오류

```json
{
  "success": false,
  "message": "올바른 이메일 형식이 아닙니다."
}
```

---

### 존재하지 않는 이메일

보안상 동일한 응답 반환

```json
{
  "success": true,
  "message": "비밀번호 재설정 메일이 발송되었습니다."
}
```

---

# 이메일 링크 예시

```text
https://your-domain.com/reset-password?token=abc123xyz
```

---

# 2. 재설정 토큰 검증 API

사용자가 이메일 링크 클릭 시 호출

## Request

- Method: `GET`
- URL:

```text
/api/v1/auth/password/verify-token?token=abc123xyz
```

---

## Success Response

```json
{
  "success": true,
  "message": "유효한 토큰입니다."
}
```

---

## Fail Response

### 만료된 토큰

```json
{
  "success": false,
  "message": "만료된 토큰입니다."
}
```

---

### 유효하지 않은 토큰

```json
{
  "success": false,
  "message": "유효하지 않은 토큰입니다."
}
```

---

# 3. 비밀번호 재설정 API

새 비밀번호 저장 API

## Request

- Method: `POST`
- URL:

```text
/api/v1/auth/password/reset
```

---

## Request Body

```json
{
  "token": "abc123xyz",
  "newPassword": "newPassword123!",
  "confirmPassword": "newPassword123!"
}
```

---

## Validation

| 필드 | 조건 |
|---|---|
| token | 필수 |
| newPassword | 필수 |
| newPassword | 최소 8자 |
| newPassword | 영문 + 숫자 + 특수문자 포함 |
| confirmPassword | newPassword와 동일 |

---

## Success Response

```json
{
  "success": true,
  "message": "비밀번호가 성공적으로 변경되었습니다."
}
```

---

## Fail Response

### 비밀번호 불일치

```json
{
  "success": false,
  "message": "비밀번호가 일치하지 않습니다."
}
```

---

### 토큰 만료

```json
{
  "success": false,
  "message": "토큰이 만료되었습니다."
}
```

---

# DB 테이블 설계

## password_reset_token

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | bigint | PK |
| user_id | bigint | 사용자 ID |
| token | varchar | UUID 토큰 |
| expired_at | datetime | 만료시간 |
| used | boolean | 사용 여부 |
| created_at | datetime | 생성시간 |

---

# 권장 보안 정책

## 1. 토큰 만료시간

- 추천: 30분

---

## 2. 토큰 1회 사용

비밀번호 변경 성공 시:

```text
used = true
```

처리

---

## 3. 비밀번호 암호화

반드시 아래 방식 사용

```text
BCryptPasswordEncoder
```

---

## 4. 기존 토큰 무효화

새 요청 발생 시 기존 토큰 모두 만료 처리 추천

---

# 전체 동작 흐름

```text
[사용자]
   ↓
비밀번호 찾기 버튼 클릭
   ↓
이메일 입력
   ↓
[백엔드]
토큰 생성 + DB 저장
   ↓
메일 발송
   ↓
[사용자]
메일 링크 클릭
   ↓
토큰 검증 API
   ↓
새 비밀번호 입력
   ↓
비밀번호 변경 API
   ↓
완료
```
