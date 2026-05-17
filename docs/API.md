# API 명세서 — EQZ Backend

> **Base URL**: `https://api.example.com`
> **인증 방식**: JWT Bearer Token (Access Token + Refresh Token)
> **Content-Type**: `application/json`

---

## 공통 응답 형식

### 성공
```json
{ "status": 200, "message": "성공", "data": {} }
```

### 실패
```json
{ "status": 400, "message": "에러 메시지", "data": null }
```

### 공통 에러 코드

| Status | 설명 |
|--------|------|
| 400 | 잘못된 요청 (파라미터 오류) |
| 401 | 인증 실패 (토큰 없음 / 만료 / 인증번호 불일치) |
| 403 | 권한 없음 / 이메일 미인증 |
| 404 | 리소스 없음 / 인증번호 만료 |
| 409 | 중복 데이터 |
| 423 | 인증 시도 잠금 |
| 429 | 요청 한도 초과 (재발송 쿨다운·발송 횟수) |
| 500 | 서버 내부 오류 |

---

## 엔드포인트 요약

### 인증 / 사용자

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/auth/email/send` | ❌ | 이메일 인증번호 발송 (PROF 가입 전) |
| `POST` | `/api/auth/email/verify` | ❌ | 이메일 인증번호 검증 |
| `POST` | `/api/auth/profsignup` | ❌ | 교수(PROF) 로컬 회원가입 (이메일 인증 선행) |
| `POST` | `/api/auth/usersignup` | ❌ | 학생(USER) 카카오 소셜 가입 완료 |
| `GET` | `/api/auth/check-nickname` | ❌ | 닉네임 중복 확인 |
| `POST` | `/api/auth/login` | ❌ | 일반 로그인 (PROF) |
| `GET` | `/oauth2/authorization/kakao` | ❌ | Kakao 소셜 로그인 (USER) |
| `POST` | `/api/auth/reissue` | ❌ | Access Token 재발급 |
| `POST` | `/api/auth/logout` | ❌ | 로그아웃 (request body의 refreshToken만 사용) |
| `GET` | `/api/users/me` | ✅ | 회원 정보 조회 |
| `PATCH` | `/api/users/me` | ✅ | 회원 정보 수정 |
| `DELETE` | `/api/users/me` | ✅ | 회원 탈퇴 |

### 교안

| Method | URL | 인증 | 권한 | 설명 |
|--------|-----|------|------|------|
| `POST` | `/api/lessons` | ✅ | PROF | 교안 생성 |
| `GET` | `/api/lessons` | ✅ | 모두 | 교안 목록 조회 (페이지네이션) |
| `GET` | `/api/lessons/{id}` | ✅ | 모두 | 교안 단건 조회 |
| `PUT` | `/api/lessons/{id}` | ✅ | PROF(본인)/ADMIN | 교안 수정 |
| `DELETE` | `/api/lessons/{id}` | ✅ | PROF(본인)/ADMIN | 교안 삭제 |
| `GET` | `/api/admin/lessons` | ✅ | ADMIN | 전체 교안 목록 조회 (페이지네이션) |

### 퀴즈

| Method | URL | 인증 | 권한 | 설명 |
|--------|-----|------|------|------|
| `POST` | `/api/quiz` | ✅ | PROF | 퀴즈 세트 생성 |
| `GET` | `/api/quiz` | ✅ | 모두 | 퀴즈 목록 조회 (페이지네이션) |
| `GET` | `/api/quiz/{quizId}` | ✅ | 모두 | 퀴즈 상세 조회 (문제 포함) |
| `PUT` | `/api/quiz/{quizId}` | ✅ | PROF(본인)/ADMIN | 퀴즈 수정 |
| `DELETE` | `/api/quiz/{quizId}` | ✅ | PROF(본인)/ADMIN | 퀴즈 삭제 |
| `POST` | `/api/quiz/{quizId}/questions` | ✅ | PROF(본인)/ADMIN | 문제 추가 |
| `PUT` | `/api/quiz/{quizId}/questions/{questionId}` | ✅ | PROF(본인)/ADMIN | 문제 수정 |
| `DELETE` | `/api/quiz/{quizId}/questions/{questionId}` | ✅ | PROF(본인)/ADMIN | 문제 삭제 |
| `POST` | `/api/quiz/{quizId}/submit` | ✅ | USER | 퀴즈 제출 |
| `GET` | `/api/quiz/wrong-answers` | ✅ | USER | 오답 목록 조회 (페이지네이션) |

---

## 1. 교수(PROF) 회원가입

**POST** `/api/auth/profsignup`

> 성공 시 `Role.PROF` + `AuthProvider.LOCAL`로 저장됩니다.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 | 유효성 |
|----------|------|------|------|--------|
| `username` | String | ✅ | 이름 | 2~20자 |
| `email` | String | ✅ | 이메일 | 이메일 형식, 중복 불가 |
| `nickname` | String | ✅ | 닉네임 | 영문·숫자·한글, 2~20자, 중복 불가 |
| `password` | String | ✅ | 비밀번호 | 8~20자, 영문+숫자+특수문자 |
| `passwordConfirm` | String | ✅ | 비밀번호 확인 | password와 일치 |

### Response

| 상황 | Status | 메시지 |
|------|--------|--------|
| 성공 | `201` | 회원가입 성공 |
| 이메일 미인증 | `403` | 이메일 인증이 완료되지 않았습니다. |
| 비밀번호 확인 불일치 | `400` | 비밀번호 확인이 일치하지 않습니다. |
| 이메일 중복 | `409` | 이미 사용 중인 이메일입니다. |
| 닉네임 중복 | `409` | 이미 사용 중인 닉네임입니다. |
| 유효성 실패 | `400` | (필드별 메시지) |

> `/api/auth/email/send` → `/api/auth/email/verify`로 이메일 인증을 완료한 뒤 30분 안에 호출해야 합니다. 자세한 흐름은 §1-3, §1-4 참고.

---

## 1-1. 학생(USER) 소셜 가입 완료

**POST** `/api/auth/usersignup`

> 카카오 동의 화면을 통과한 신규 유저가 이름·이메일·닉네임을 입력하고 호출하는 엔드포인트입니다.
> 성공 시 `Role.USER` + `AuthProvider.KAKAO`로 저장되고 즉시 JWT가 발급됩니다.
> 흐름은 §3 소셜 로그인을 참조하세요.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 | 유효성 |
|----------|------|------|------|--------|
| `pendingToken` | String | ✅ | 백엔드가 발급한 10분짜리 임시 토큰 (`?pendingToken=...`로 전달받음) | |
| `username` | String | ✅ | 이름 (카카오 `profile_nickname`이 기본값, 수정 가능) | 2~20자 |
| `email` | String | ✅ | 이메일 | 이메일 형식, 중복 불가 |
| `nickname` | String | ✅ | 닉네임 | 영문·숫자·한글, 2~20자, 중복 불가 |

### Response (201)

```json
{
  "status": 201,
  "message": "회원가입 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| pending 토큰 만료·위변조 | `401` | 소셜 가입 정보가 만료되었거나 유효하지 않습니다. 카카오 로그인을 다시 시도해 주세요. |
| 이메일 중복 | `409` | 이미 사용 중인 이메일입니다. |
| 닉네임 중복 | `409` | 이미 사용 중인 닉네임입니다. |

---

## 1-2. 닉네임 중복 확인

**GET** `/api/auth/check-nickname?nickname={nickname}`

> 회원가입 폼에서 입력 중 실시간 호출되는 엔드포인트입니다.
> 중복 여부를 에러가 아닌 `200 + available` 필드로 반환합니다.

### Query Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `nickname` | String | ✅ | 확인할 닉네임 |

### Response (200)

```json
// 사용 가능
{ "status": 200, "message": "사용 가능한 닉네임입니다.", "data": { "available": true } }

// 중복
{ "status": 200, "message": "이미 사용 중인 닉네임입니다.", "data": { "available": false } }
```

---

## 1-3. 이메일 인증번호 발송

**POST** `/api/auth/email/send`

> PROF 회원가입 전 이메일 소유 확인용. 인증번호는 6자리 숫자이며 5분간 유효합니다.
> 60초 쿨다운, 1시간 윈도우 5회 제한이 적용됩니다.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 | 유효성 |
|----------|------|------|------|--------|
| `email` | String | ✅ | 인증번호를 받을 이메일 | 이메일 형식 |

### Response (200)

```json
{ "status": 200, "message": "인증번호가 전송되었습니다.", "data": null }
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 이미 가입된 이메일 | `409` | 이미 사용 중인 이메일입니다. |
| 잠금 상태 | `423` | 인증 시도가 잠금 상태입니다. 잠시 후 다시 시도해 주세요. |
| 재발송 쿨다운 | `429` | 재전송 대기 시간이 남아 있습니다. 잠시 후 다시 시도해 주세요. |
| 1시간 발송 한도 초과 | `429` | 인증번호 발송 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요. |
| 유효성 실패 | `400` | (필드별 메시지) |

---

## 1-4. 이메일 인증번호 검증

**POST** `/api/auth/email/verify`

> 검증 성공 시 서버는 이메일 인증 완료 상태를 30분간 유지합니다. 같은 이메일로 `/api/auth/profsignup`을 호출하면 가입이 완료됩니다.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 | 유효성 |
|----------|------|------|------|--------|
| `email` | String | ✅ | 인증번호 발송 시 사용한 이메일 | 이메일 형식 |
| `code` | String | ✅ | 6자리 인증번호 | 숫자 6자리 |

### Response (200)

```json
{ "status": 200, "message": "이메일 인증이 완료되었습니다.", "data": null }
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 인증번호 불일치 | `401` | 인증번호가 일치하지 않습니다. |
| 인증번호 없음/만료 | `404` | 인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해 주세요. |
| 시도 횟수 초과 | `423` | 인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요. |
| 잠금 상태 | `423` | 인증 시도가 잠금 상태입니다. 잠시 후 다시 시도해 주세요. |
| 유효성 실패 | `400` | (필드별 메시지) |

---

## 2. 일반 로그인

**POST** `/api/auth/login`

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `email` | String | ✅ | 이메일 |
| `password` | String | ✅ | 비밀번호 |

### Response (200)

```json
{
  "status": 200,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  }
}
```

> - Access Token 유효기간: **30분**
> - Refresh Token 유효기간: **7일**

---

## 3. 소셜 로그인 (Kakao OpenID Connect)

**GET** `/oauth2/authorization/kakao` → Kakao OIDC 인증 페이지로 리다이렉트

카카오 인증 완료 후 백엔드(`OAuth2SuccessHandler`)는 **DB에 해당 카카오 계정이 존재하는지**에 따라 두 가지 경로로 분기합니다.

### 기존 유저 (DB에 존재)
바로 JWT 발급.

```
{redirect-uri}?accessToken=eyJ...&refreshToken=eyJ...
# 예: http://localhost:5174/oauth2/callback?accessToken=...&refreshToken=...
```

### 신규 유저 (DB에 없음)
DB 저장은 하지 않고, **10분짜리 pending 토큰**과 함께 정보 입력 페이지로 리다이렉트. 프론트는 이름 필드를 `kakaoName`으로 pre-fill하고 사용자가 이메일·닉네임을 입력한 뒤 `POST /api/auth/usersignup` 호출(§1-1).

```
{register-uri}?pendingToken=eyJ...&kakaoName=홍길동
# 예: http://localhost:5174/oauth2/register?pendingToken=...&kakaoName=...
```

| pending 토큰 클레임 | 값 |
|--------------------|----|
| `sub` | providerId (카카오 OIDC `sub`) |
| `type` | `PENDING_SOCIAL` |
| `provider` | `KAKAO` |
| `name` | 카카오 `profile_nickname` (pre-fill용) |
| 유효기간 | 10분 |

---

## 4. 토큰 재발급

**POST** `/api/auth/reissue`

> Refresh Token Rotation 적용 — 재발급 시 기존 토큰 폐기

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `refreshToken` | String | ✅ | 기존 Refresh Token |

### Response (200)

```json
{
  "status": 200,
  "message": "토큰 재발급 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  }
}
```

---

## 5. 로그아웃

**POST** `/api/auth/logout`

> request body의 `refreshToken`만으로 동작합니다. Authorization 헤더(access token)는 검증·요구되지 않습니다 — access token이 만료된 상태에서도 로그아웃 가능.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `refreshToken` | String | ✅ | 폐기할 Refresh Token |

### Response (200)

```json
{ "status": 200, "message": "로그아웃 성공", "data": null }
```

---

## 6. 회원 정보 조회

**GET** `/api/users/me` | 🔒 인증 필요

### Response (200)

```json
{
  "status": 200,
  "message": "조회 성공",
  "data": {
    "id": 1,
    "username": "홍길동",
    "email": "hong@example.com",
    "provider": "LOCAL",
    "role": "USER",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

| 필드 | 설명 |
|------|------|
| `provider` | `LOCAL` / `KAKAO` |
| `role` | `USER` / `PROF` / `ADMIN` |

---

## 7. 회원 정보 수정

**PATCH** `/api/users/me` | 🔒 인증 필요

### Request Body

| 파라미터 | 타입 | 필수 | 설명 | 유효성 |
|----------|------|------|------|--------|
| `username` | String | ❌ | 새 닉네임 | 2~20자 |
| `currentPassword` | String | ❌ | 현재 비밀번호 (비밀번호 변경 시 필수) | |
| `newPassword` | String | ❌ | 새 비밀번호 | 8~20자, 영문+숫자+특수문자 |

### Response (200)

```json
{
  "status": 200,
  "message": "회원 정보 수정 성공",
  "data": {
    "id": 1,
    "username": "새닉네임",
    "email": "hong@example.com",
    "provider": "LOCAL",
    "role": "USER",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

---

## 8. 회원 탈퇴

**DELETE** `/api/users/me` | 🔒 인증 필요

### Request Body (optional)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `password` | String | ❌ | 현재 비밀번호 (LOCAL 계정만 해당) |

### Response (200)

```json
{ "status": 200, "message": "회원 탈퇴 성공", "data": null }
```

---

## 9. 교안 생성

**POST** `/api/lessons` | 🔒 PROF

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `title` | String | ✅ | 교안 제목 |
| `description` | String | ❌ | 교안 설명 |

### Response (201)

```json
{
  "status": 201,
  "message": "교안 생성 성공",
  "data": {
    "id": 1,
    "title": "3장. 프로세스와 스레드",
    "description": "프로세스와 스레드의 개념을 학습합니다.",
    "createdById": 10,
    "createdByName": "김교수",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

## 10. 교안 목록 조회

**GET** `/api/lessons` | 🔒 인증 필요

> 기본값: `size=10`, `sort=createdAt,DESC`

### Query Parameters (Pageable)

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `page` | Integer | 페이지 번호 (0부터, 기본값 0) |
| `size` | Integer | 페이지 크기 (기본값 10) |
| `sort` | String | 정렬 기준 (기본값 `createdAt,DESC`) |

### Response (200)

```json
{
  "status": 200,
  "message": "교안 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "3장. 프로세스와 스레드",
        "description": "프로세스와 스레드의 개념을 학습합니다.",
        "createdById": 10,
        "createdByName": "김교수",
        "createdAt": "2025-01-01T00:00:00",
        "updatedAt": "2025-01-01T00:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

---

## 11. 교안 단건 조회

**GET** `/api/lessons/{id}` | 🔒 인증 필요

### Response (200)

```json
{
  "status": 200,
  "message": "교안 조회 성공",
  "data": {
    "id": 1,
    "title": "3장. 프로세스와 스레드",
    "description": "프로세스와 스레드의 개념을 학습합니다.",
    "createdById": 10,
    "createdByName": "김교수",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

## 12. 교안 수정

**PUT** `/api/lessons/{id}` | 🔒 PROF(본인)/ADMIN

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `title` | String | ✅ | 교안 제목 |
| `description` | String | ❌ | 교안 설명 |

### Response (200)

```json
{
  "status": 200,
  "message": "교안 수정 성공",
  "data": { "id": 1, "title": "수정된 제목", "..." : "..." }
}
```

---

## 13. 교안 삭제

**DELETE** `/api/lessons/{id}` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{ "status": 200, "message": "교안 삭제 성공", "data": null }
```

---

## 14. 전체 교안 목록 조회 (관리자)

**GET** `/api/admin/lessons` | 🔒 ADMIN

> 기본값: `size=10`, `sort=createdAt,DESC`

### Query Parameters (Pageable)

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `page` | Integer | 페이지 번호 (0부터, 기본값 0) |
| `size` | Integer | 페이지 크기 (기본값 10) |
| `sort` | String | 정렬 기준 (기본값 `createdAt,DESC`) |

### Response (200)

```json
{
  "status": 200,
  "message": "전체 교안 목록 조회 성공",
  "data": {
    "content": [ { "id": 1, "title": "...", "..." : "..." } ],
    "totalElements": 50,
    "totalPages": 5,
    "size": 10,
    "number": 0
  }
}
```

---

## 15. 퀴즈 세트 생성

**POST** `/api/quiz` | 🔒 PROF

### Request Body

```json
{
  "title": "3장 운영체제 기초 퀴즈",
  "description": "3장 내용 복습용 퀴즈입니다."
}
```

### Response (201)

```json
{
  "status": 201,
  "message": "퀴즈 생성 성공",
  "data": {
    "id": 1,
    "title": "3장 운영체제 기초 퀴즈",
    "description": "3장 내용 복습용 퀴즈입니다.",
    "professorId": 10,
    "professorName": "김교수",
    "questionCount": 0,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

## 16. 퀴즈 목록 조회

**GET** `/api/quiz` | 🔒 인증 필요

> 기본값: `size=10`, `sort=createdAt,DESC`

### Query Parameters (Pageable)

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `page` | Integer | 페이지 번호 (0부터, 기본값 0) |
| `size` | Integer | 페이지 크기 (기본값 10) |
| `sort` | String | 정렬 기준 (기본값 `createdAt,DESC`) |

### Response (200)

```json
{
  "status": 200,
  "message": "퀴즈 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "3장 운영체제 기초 퀴즈",
        "description": "3장 내용 복습용 퀴즈입니다.",
        "professorId": 10,
        "professorName": "김교수",
        "questionCount": 5,
        "createdAt": "2025-01-01T00:00:00",
        "updatedAt": "2025-01-01T00:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

---

## 17. 퀴즈 상세 조회

**GET** `/api/quiz/{quizId}` | 🔒 인증 필요

### Response (200)

```json
{
  "status": 200,
  "message": "퀴즈 조회 성공",
  "data": {
    "id": 1,
    "title": "3장 운영체제 기초 퀴즈",
    "description": "3장 내용 복습용 퀴즈입니다.",
    "professorId": 10,
    "professorName": "김교수",
    "questions": [
      {
        "id": 5,
        "questionText": "프로세스와 스레드의 차이점은?",
        "questionType": "MULTIPLE_CHOICE",
        "score": 10,
        "options": [
          { "id": 1, "optionText": "프로세스는 독립된 메모리 공간을 가진다." },
          { "id": 2, "optionText": "스레드는 서로 다른 힙을 사용한다." }
        ],
        "anchorId": 3,
        "anchorTitle": "3장. 프로세스와 스레드",
        "lessonPage": 12,
        "lessonParagraph": 3
      }
    ],
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

> ⚠️ `correctAnswer`는 학생에게 노출되지 않습니다.

---

## 18. 퀴즈 수정

**PUT** `/api/quiz/{quizId}` | 🔒 PROF(본인)/ADMIN

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `title` | String | ✅ | 퀴즈 제목 |
| `description` | String | ❌ | 퀴즈 설명 |

### Response (200)

```json
{
  "status": 200,
  "message": "퀴즈 수정 성공",
  "data": { "id": 1, "title": "수정된 제목", "..." : "..." }
}
```

---

## 19. 퀴즈 삭제

**DELETE** `/api/quiz/{quizId}` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{ "status": 200, "message": "퀴즈 삭제 성공", "data": null }
```

---

## 20. 문제 추가

**POST** `/api/quiz/{quizId}/questions` | 🔒 PROF(본인)/ADMIN

> 교수는 문제가 교안의 어느 페이지/문단에서 왔는지 `anchorId`, `lessonPage`, `lessonParagraph`로 지정합니다.

### Request Body — 객관식 예시

```json
{
  "questionText": "프로세스와 스레드의 차이점은?",
  "questionType": "MULTIPLE_CHOICE",
  "options": [
    { "optionText": "프로세스는 독립된 메모리 공간을 가진다.", "correct": true },
    { "optionText": "스레드는 서로 다른 힙을 사용한다.", "correct": false },
    { "optionText": "프로세스 생성이 스레드보다 빠르다.", "correct": false },
    { "optionText": "스레드는 컨텍스트 스위칭 비용이 더 크다.", "correct": false }
  ],
  "correctAnswer": "프로세스는 독립된 메모리 공간을 가진다.",
  "explanation": "프로세스는 독립적인 메모리 공간을 가지지만, 스레드는 같은 프로세스 내에서 힙/데이터 영역을 공유합니다.",
  "score": 10,
  "anchorId": 3,
  "lessonPage": 12,
  "lessonParagraph": 3
}
```

### Request Body — 단답형 예시

```json
{
  "questionText": "CPU 스케줄링 알고리즘 중 비선점 방식을 하나 서술하시오.",
  "questionType": "SHORT_ANSWER",
  "options": [],
  "correctAnswer": "FCFS",
  "explanation": "FCFS(First Come First Served)는 도착 순서대로 처리하는 비선점 스케줄링입니다.",
  "score": 5,
  "anchorId": 3,
  "lessonPage": 15,
  "lessonParagraph": 1
}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `questionText` | String | ✅ | 문제 내용 |
| `questionType` | String | ✅ | `MULTIPLE_CHOICE` / `SHORT_ANSWER` |
| `options` | Array | ❌ | 객관식 보기 목록 |
| `correctAnswer` | String | ✅ | 정답 텍스트 |
| `explanation` | String | ❌ | 해설 |
| `score` | Integer | ✅ | 배점 (0 이상) |
| `anchorId` | Long | ❌ | 참조 교안 ID |
| `lessonPage` | Integer | ❌ | 교안 내 페이지 번호 |
| `lessonParagraph` | Integer | ❌ | 교안 내 문단 번호 |

### Response (201)

```json
{
  "status": 201,
  "message": "문제 추가 성공",
  "data": {
    "id": 5,
    "questionText": "프로세스와 스레드의 차이점은?",
    "questionType": "MULTIPLE_CHOICE",
    "score": 10,
    "options": [
      { "id": 1, "optionText": "프로세스는 독립된 메모리 공간을 가진다." },
      { "id": 2, "optionText": "스레드는 서로 다른 힙을 사용한다." }
    ],
    "anchorId": 3,
    "anchorTitle": "3장. 프로세스와 스레드",
    "lessonPage": 12,
    "lessonParagraph": 3
  }
}
```

---

## 21. 문제 수정

**PUT** `/api/quiz/{quizId}/questions/{questionId}` | 🔒 PROF(본인)/ADMIN

> `questionType`은 수정 불가합니다.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `questionText` | String | ✅ | 문제 내용 |
| `options` | Array | ❌ | 객관식 보기 목록 |
| `correctAnswer` | String | ✅ | 정답 텍스트 |
| `explanation` | String | ❌ | 해설 |
| `score` | Integer | ✅ | 배점 (0 이상) |
| `anchorId` | Long | ❌ | 참조 교안 ID |
| `lessonPage` | Integer | ❌ | 교안 내 페이지 번호 |
| `lessonParagraph` | Integer | ❌ | 교안 내 문단 번호 |

### Response (200)

```json
{
  "status": 200,
  "message": "문제 수정 성공",
  "data": { "id": 5, "questionText": "수정된 문제 내용", "..." : "..." }
}
```

---

## 22. 문제 삭제

**DELETE** `/api/quiz/{quizId}/questions/{questionId}` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{ "status": 200, "message": "문제 삭제 성공", "data": null }
```

---

## 23. 퀴즈 제출

**POST** `/api/quiz/{quizId}/submit` | 🔒 USER

> 퀴즈당 1회만 제출 가능합니다. 재제출 시 `409` 반환.

### Request Body

```json
{
  "answers": [
    { "questionId": 5, "studentAnswer": "프로세스는 독립된 메모리 공간을 가진다." },
    { "questionId": 6, "studentAnswer": "FCFS" }
  ]
}
```

### Response (200)

```json
{
  "status": 200,
  "message": "퀴즈 제출 성공",
  "data": {
    "submissionId": 100,
    "quizId": 1,
    "quizTitle": "3장 운영체제 기초 퀴즈",
    "totalScore": 15,
    "correctCount": 2,
    "totalQuestions": 2,
    "answers": [
      {
        "questionId": 5,
        "questionText": "프로세스와 스레드의 차이점은?",
        "studentAnswer": "프로세스는 독립된 메모리 공간을 가진다.",
        "correctAnswer": "프로세스는 독립된 메모리 공간을 가진다.",
        "correct": true,
        "score": 10
      },
      {
        "questionId": 6,
        "questionText": "CPU 스케줄링 알고리즘 중 비선점 방식을 하나 서술하시오.",
        "studentAnswer": "FCFS",
        "correctAnswer": "FCFS",
        "correct": true,
        "score": 5
      }
    ],
    "submittedAt": "2025-01-01T10:00:00"
  }
}
```

---

## 24. 오답 목록 조회

**GET** `/api/quiz/wrong-answers` | 🔒 USER

> 학생이 제출한 퀴즈에서 틀린 문제 목록을 조회합니다.
> 각 오답에 교수가 지정한 **교안 참조 정보(lessonRef)**가 포함됩니다.
> 기본값: `size=10`, 정렬 기준: 제출 일시 DESC

### Query Parameters (Pageable)

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `page` | Integer | 페이지 번호 (0부터, 기본값 0) |
| `size` | Integer | 페이지 크기 (기본값 10) |

### Response (200)

```json
{
  "status": 200,
  "message": "오답 목록 조회 성공",
  "data": {
    "content": [
      {
        "submissionId": 100,
        "quizId": 1,
        "quizTitle": "3장 운영체제 기초 퀴즈",
        "questionId": 7,
        "questionText": "선점 스케줄링과 비선점 스케줄링의 차이는?",
        "questionType": "SHORT_ANSWER",
        "options": [],
        "studentAnswer": "처리 순서",
        "correctAnswer": "CPU를 강제로 빼앗을 수 있는지 여부",
        "explanation": "선점 스케줄링은 실행 중인 프로세스에서 CPU를 강제 회수할 수 있습니다.",
        "lessonRef": {
          "lessonId": 3,
          "lessonTitle": "3장. 프로세스와 스레드",
          "lessonPage": 18,
          "lessonParagraph": 2
        },
        "submittedAt": "2025-01-01T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

| 필드 | 설명 |
|------|------|
| `options` | 객관식 보기 텍스트 목록 (`List<String>`), 단답형은 빈 배열 |
| `lessonRef.lessonId` | 교안 ID |
| `lessonRef.lessonTitle` | 교안 제목 |
| `lessonRef.lessonPage` | 교수가 지정한 교안 페이지 번호 |
| `lessonRef.lessonParagraph` | 교수가 지정한 교안 문단 번호 |

> `lessonRef`는 문제 생성 시 `anchorId`를 지정하지 않은 경우 `null`입니다.

---

## DB 테이블 설계

### users

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `username` | VARCHAR(20) | 이름 |
| `nickname` | VARCHAR(20) | 닉네임 (UNIQUE) |
| `email` | VARCHAR | 이메일 (UNIQUE) |
| `password` | VARCHAR | NULL (소셜 로그인) |
| `role` | VARCHAR(10) | `USER` / `PROF` / `ADMIN` |
| `provider` | VARCHAR(10) | `LOCAL` / `KAKAO` |
| `provider_id` | VARCHAR | 소셜 로그인 시 OIDC `sub`, LOCAL은 NULL |

> 자세한 스키마는 [docs/generated/db-schema.md](generated/db-schema.md) 참조.

### lecture_material (교안)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `professor_id` | BIGINT | FK → users |
| `title` | VARCHAR(200) | 교안 제목 |
| `description` | TEXT | 교안 설명 |

### quiz (퀴즈 세트)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `professor_id` | BIGINT | FK → users |
| `title` | VARCHAR(200) | 퀴즈 제목 |
| `description` | VARCHAR(500) | 퀴즈 설명 |

### quiz_q (퀴즈 문제)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `quiz_id` | BIGINT | FK → quiz |
| `anchor_id` | BIGINT | FK → lecture_material (NULL 가능) |
| `question_text` | TEXT | 문제 내용 |
| `q_type` | question_type | `MULTIPLE_CHOICE` / `SHORT_ANSWER` |
| `score` | INT | 배점 |
| `correct_answer` | TEXT | 정답 |
| `explanation` | TEXT | 해설 |
| `lesson_page` | INT | 교수가 지정한 교안 페이지 |
| `lesson_paragraph` | INT | 교수가 지정한 교안 문단 |

### quiz_opt (객관식 보기)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `question_id` | BIGINT | FK → quiz_q |
| `option_text` | VARCHAR(500) | 보기 내용 |
| `is_correct` | BOOLEAN | 정답 여부 |

### quiz_sub (제출)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `quiz_id` | BIGINT | FK → quiz |
| `student_id` | BIGINT | FK → users |
| `total_score` | INT | 총점 |
| `submitted_at` | TIMESTAMPTZ | 제출 일시 |

### quiz_sub_answer (문제별 답안)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `submission_id` | BIGINT | FK → quiz_sub |
| `question_id` | BIGINT | FK → quiz_q |
| `student_answer` | VARCHAR(500) | 학생 답안 |
| `is_correct` | BOOLEAN | 정답 여부 |
| `score` | INT | 획득 점수 |

### refresh_tokens

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | FK → users |
| `token` | VARCHAR(512) | Refresh Token |
| `expires_at` | TIMESTAMPTZ | 만료 일시 |
