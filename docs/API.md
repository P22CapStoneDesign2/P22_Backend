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
| 400 | 잘못된 요청 (파라미터 오류 / 요청 본문 누락·형식 오류) |
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
| `POST` | `/api/auth/profsignup` | ❌ | 교수(PROF) 로컬 회원가입 (이메일 인증 선행) — 가입 직후 PENDING 상태 |
| `POST` | `/api/auth/usersignup` | ❌ | 학생(USER) 카카오 소셜 가입 완료 |
| `GET` | `/api/auth/check-nickname` | ❌ | 닉네임 중복 확인 |
| `POST` | `/api/auth/login` | ❌ | 일반 로그인 (PROF) |
| `GET` | `/oauth2/authorization/kakao` | ❌ | Kakao 소셜 로그인 (USER) |
| `POST` | `/api/auth/reissue` | ❌ | Access Token 재발급 |
| `POST` | `/api/auth/logout` | ❌ | 로그아웃 (request body의 refreshToken만 사용) |
| `GET` | `/api/users/me` | ✅ | 회원 정보 조회 (status 포함) |
| `PATCH` | `/api/users/me` | ✅ | 회원 정보 수정 |
| `DELETE` | `/api/users/me` | ✅ | 회원 탈퇴 |

### 관리자 (ADMIN)

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| `GET` | `/api/admin/lessons` | ✅ | 전체 강의 목록 조회 (페이지네이션) |
| `GET` | `/api/admin/professors/pending` | ✅ | 승인 대기 PROF 목록 (페이지네이션) |
| `POST` | `/api/admin/professors/{id}/approve` | ✅ | PROF 계정 승인 (ACTIVE로 변경) |
| `POST` | `/api/admin/professors/{id}/reject` | ✅ | PROF 계정 거절 (REJECTED로 변경) |
| `PATCH` | `/api/admin/professors/{id}/status` | ✅ | PROF 계정 상태 임의 변경 (REJECTED → ACTIVE 등) |

### 강의

| Method | URL | 인증 | 권한 | 설명 |
|--------|-----|------|------|------|
| `POST` | `/api/lessons` | ✅ | PROF | 강의 생성 |
| `GET` | `/api/lessons` | ✅ | 모두 | 강의 목록 조회 (페이지네이션) — PROF: 본인 생성 강의만, USER/ADMIN: 전체 |
| `GET` | `/api/lessons/{lessonId}` | ✅ | 모두 | 강의 단건 조회 |
| `PUT` | `/api/lessons/{lessonId}` | ✅ | PROF(본인)/ADMIN | 강의 수정 |
| `DELETE` | `/api/lessons/{lessonId}` | ✅ | PROF(본인)/ADMIN | 강의 소프트 삭제 — 하위 교안·퀴즈 연쇄 소프트 삭제 |
| `GET` | `/api/admin/lessons` | ✅ | ADMIN | 전체 강의 목록 조회 (페이지네이션) |
| `GET` | `/api/lessons/my` | ✅ | USER | 학생 본인이 승인받은 강의 목록 |
| `POST` | `/api/lessons/{lessonId}/enrollments` | ✅ | USER | 강의 수강 신청 |
| `DELETE` | `/api/lessons/{lessonId}/enrollments` | ✅ | USER | 본인 신청 취소 (PENDING일 때만) |
| `GET` | `/api/lessons/{lessonId}/enrollments` | ✅ | PROF(본인)/ADMIN | 강의 신청 목록 조회 |
| `POST` | `/api/lessons/{lessonId}/enrollments/{enrollmentId}/approve` | ✅ | PROF(본인)/ADMIN | 신청 수락 |
| `POST` | `/api/lessons/{lessonId}/enrollments/{enrollmentId}/reject` | ✅ | PROF(본인)/ADMIN | 신청 거절 |

### 교안

| Method | URL | 인증 | 권한 | 설명 |
|--------|-----|------|------|------|

| `POST` | `/api/lessons` | ✅ | PROF | 교안 생성 |
| `GET` | `/api/lessons` | ✅ | 모두 | 교안 목록 조회 (페이지네이션) |
| `GET` | `/api/lessons/{id}` | ✅ | 모두 | 교안 단건 조회 |
| `PUT` | `/api/lessons/{id}` | ✅ | PROF(본인)/ADMIN | 교안 수정 |
| `DELETE` | `/api/lessons/{id}` | ✅ | PROF(본인)/ADMIN | 교안 삭제 |
| `POST` | `/api/lessons/{lessonId}/pdf` | ✅ | PROF(본인)/ADMIN | 교안 PDF 업로드 (`multipart/form-data`) |
| `GET` | `/api/lessons/{lessonId}/pdf` | ✅ | 모두 | 교안 PDF 목록 조회 |
| `DELETE` | `/api/lessons/pdf/{pdfId}` | ✅ | PROF(업로더)/ADMIN | 교안 PDF 삭제 |
| `GET` | `/api/admin/lessons` | ✅ | ADMIN | 전체 교안 목록 조회 (페이지네이션) |

| `POST` | `/api/lessons/{lessonId}/materials` | ✅ | PROF(강의 소유자) | 교안 생성 |
| `GET` | `/api/lessons/{lessonId}/materials` | ✅ | 모두 | 교안 목록 조회 (USER: 수강 승인된 강의만) |
| `GET` | `/api/lessons/{lessonId}/materials/{materialId}` | ✅ | 모두 | 교안 단건 조회 |
| `PUT` | `/api/lessons/{lessonId}/materials/{materialId}` | ✅ | PROF(본인)/ADMIN | 교안 수정 |
| `DELETE` | `/api/lessons/{lessonId}/materials/{materialId}` | ✅ | PROF(본인)/ADMIN | 교안 소프트 삭제 — 하위 퀴즈 연쇄 소프트 삭제 |


### 퀴즈

| Method | URL | 인증 | 권한 | 설명 |
|--------|-----|------|------|------|
| `POST` | `/api/quiz` | ✅ | PROF | 퀴즈 세트 생성 (교안 지정 필수) |
| `GET` | `/api/quiz` | ✅ | 모두 | 퀴즈 목록 조회 — USER: APPROVED 강의 소속 교안만, PROF: 본인 교안만, ADMIN: 전체. `?materialId` 필터 지원 |
| `GET` | `/api/quiz/{quizId}` | ✅ | USER(수강 승인 강의)/PROF/ADMIN | 퀴즈 상세 조회 (문제 포함, 정답 미노출) |
| `GET` | `/api/quiz/{quizId}/edit` | ✅ | PROF(본인)/ADMIN | 퀴즈 수정용 조회 (정답·해설 포함, preload) |
| `PUT` | `/api/quiz/{quizId}` | ✅ | PROF(본인)/ADMIN | 퀴즈 수정 (`materialId` 변경 불가) |
| `DELETE` | `/api/quiz/{quizId}` | ✅ | PROF(본인)/ADMIN | 퀴즈 삭제 |
| `POST` | `/api/quiz/{quizId}/questions` | ✅ | PROF(본인)/ADMIN | 문제 추가 |
| `PUT` | `/api/quiz/{quizId}/questions/{questionId}` | ✅ | PROF(본인)/ADMIN | 문제 수정 |
| `DELETE` | `/api/quiz/{quizId}/questions/{questionId}` | ✅ | PROF(본인)/ADMIN | 문제 삭제 |
| `POST` | `/api/quiz/{quizId}/submit` | ✅ | USER(수강 승인 강의) | 퀴즈 제출 |
| `GET` | `/api/quiz/wrong-answers` | ✅ | USER | 오답 목록 조회 (페이지네이션) |

---

## 1. 교수(PROF) 회원가입

**POST** `/api/auth/profsignup`

> 성공 시 `Role.PROF` + `AuthProvider.LOCAL`로 저장되며, 계정 상태는 `PENDING`으로 설정됩니다.
> ADMIN 승인 전까지 PROF 전용 기능(교안 생성, 퀴즈 생성 등)을 사용할 수 없습니다.
> 가입 직후 토큰은 정상 발급되므로 `GET /api/users/me`로 상태를 확인할 수 있습니다.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 | 유효성 |
|----------|------|------|------|--------|
| `username` | String | ✅ | 이름 | 2~20자 |
| `email` | String | ✅ | 이메일 | 이메일 형식, 중복 불가 |
| `nickname` | String | ✅ | 닉네임 | 영문·숫자·한글, 2~20자, 중복 불가 |
| `password` | String | ✅ | 비밀번호 | 8~20자, 영문+숫자+특수문자 |
| `passwordConfirm` | String | ✅ | 비밀번호 확인 | password와 일치 |

### Response (201)

```json
{
  "status": 201,
  "message": "회원가입 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "status": "PENDING"
  }
}
```

| 상황 | Status | 에러코드 | 메시지 |
|------|--------|----------|--------|
| 성공 | `201` | — | 회원가입 성공 |
| 이메일 미인증 | `403` | — | 이메일 인증이 완료되지 않았습니다. |
| 비밀번호 확인 불일치 | `400` | — | 비밀번호 확인이 일치하지 않습니다. |
| 이메일 중복 | `409` | — | 이미 사용 중인 이메일입니다. |
| 가입 거절된 이메일 재가입 | `409` | `EMAIL_REJECTED` | 가입이 거절된 이메일입니다. 관리자에게 문의해 주세요. |
| 닉네임 중복 | `409` | — | 이미 사용 중인 닉네임입니다. |
| 유효성 실패 | `400` | — | (필드별 메시지) |

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
    "role": "PROF",
    "status": "PENDING",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

| 필드 | 설명 |
|------|------|
| `provider` | `LOCAL` / `KAKAO` |
| `role` | `USER` / `PROF` / `ADMIN` |
| `status` | `PENDING` / `ACTIVE` / `REJECTED` — PROF 계정 승인 상태. USER·ADMIN은 항상 `ACTIVE` |

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
    "status": "ACTIVE",
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

## 9. 강의 생성

**POST** `/api/lessons` | 🔒 PROF

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `title` | String | ✅ | 강의 제목 |
| `description` | String | ❌ | 강의 설명 |

### Response (201)

```json
{
  "status": 201,
  "message": "강의 생성 성공",
  "data": {
    "id": 5,
    "title": "운영체제 기초",
    "description": "운영체제의 핵심 개념을 학습합니다.",
    "createdById": 10,
    "createdByName": "김교수",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

## 10. 강의 목록 조회

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
  "message": "강의 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 5,
        "title": "운영체제 기초",
        "description": "운영체제의 핵심 개념을 학습합니다.",
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

## 11. 강의 단건 조회

**GET** `/api/lessons/{lessonId}` | 🔒 인증 필요

### Response (200)

```json
{
  "status": 200,
  "message": "강의 조회 성공",
  "data": {
    "id": 5,
    "title": "운영체제 기초",
    "description": "운영체제의 핵심 개념을 학습합니다.",
    "createdById": 10,
    "createdByName": "김교수",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

## 12. 강의 수정

**PUT** `/api/lessons/{lessonId}` | 🔒 PROF(본인)/ADMIN

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `title` | String | ✅ | 강의 제목 |
| `description` | String | ❌ | 강의 설명 |

### Response (200)

```json
{
  "status": 200,
  "message": "강의 수정 성공",
  "data": { "id": 5, "title": "수정된 제목", "..." : "..." }
}
```

---

## 13. 강의 삭제

**DELETE** `/api/lessons/{lessonId}` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{ "status": 200, "message": "강의 삭제 성공", "data": null }
```

---


## 13-1. 교안 PDF

> PDF 바이너리는 Supabase Storage 버킷 `lesson-pdf`에 저장하고, 메타데이터는 `lesson_pdfs` 테이블에 저장합니다.  
> 저장 경로: `{lessonId}/{uuid}.pdf` (버킷 내부 상대 경로)

### PDF 업로드

**POST** `/api/lessons/{lessonId}/pdf` | 🔒 PROF(교안 작성자)/ADMIN

**Content-Type:** `multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `file` | File | ✅ | PDF 파일 (`.pdf`, `application/pdf`, 최대 50MB) |

### Response (201)

```json
{
  "status": 201,
  "message": "PDF 업로드 성공",
  "data": {
    "pdfId": 1,
    "lessonId": 1,
    "originalFileName": "1주차.pdf",
    "savedFileName": "3f2c1d2a-....pdf",
    "fileUrl": "https://{project}.supabase.co/storage/v1/object/public/lesson-pdf/1/3f2c1d2a-....pdf",
    "fileSize": 102400,
    "uploadedById": 1,
    "uploadedAt": "2026-05-22T14:30:12"
  }
}
```

| 상황 | Status | ErrorCode 메시지 |
|------|--------|------------------|
| 교안 없음 | `404` | 존재하지 않는 교안입니다. |
| PDF 형식 오류 | `400` | PDF 파일만 업로드할 수 있습니다. |
| 용량 초과 | `400` | 파일 크기는 50MB를 초과할 수 없습니다. |
| Storage 업로드 실패 | `500` | 파일 업로드에 실패했습니다. |

### PDF 목록 조회

**GET** `/api/lessons/{lessonId}/pdf` | 🔒 인증 필요

### Response (200)

```json
{
  "status": 200,
  "message": "PDF 목록 조회 성공",
  "data": [
    {
      "pdfId": 1,
      "lessonId": 1,
      "originalFileName": "1주차.pdf",
      "savedFileName": "3f2c1d2a-....pdf",
      "fileUrl": "https://...",
      "fileSize": 102400,
      "uploadedById": 1,
      "uploadedAt": "2026-05-22T14:30:12"
    }
  ]
}
```

### PDF 삭제

**DELETE** `/api/lessons/pdf/{pdfId}` | 🔒 PROF(업로드한 교수)/ADMIN

### Response (200)

```json
{ "status": 200, "message": "PDF 삭제 성공", "data": null }
```

| 상황 | Status | ErrorCode 메시지 |
|------|--------|------------------|
| PDF 없음 | `404` | 존재하지 않는 PDF입니다. |
| 권한 없음 | `403` | 접근 권한이 없습니다. |
| Storage 삭제 실패 | `500` | 파일 삭제에 실패했습니다. |

---

## 13-2. PDF 교안 뷰어

> 응답 envelope: `{ "success": true, "data": { ... } }` / 실패 시 `{ "success": false, "errorCode": "...", "message": "..." }`  
> `lectureId`는 기존 `lessonId`와 동일한 강의 식별자입니다.  
> 페이지 이미지 URL은 Storage 경로 `{storagePath}/pages/{pageNumber}.png` 기준으로 생성합니다.

### 강의별 교안(PDF) 목록

**GET** `/api/lectures/{lectureId}/materials` | 🔒 USER(승인 수강)/PROF(본인 업로드)/ADMIN

### Response (200)

```json
{
  "success": true,
  "data": [
    {
      "materialId": 1,
      "title": "1주차.pdf",
      "pageCount": 12,
      "thumbnailUrl": "https://.../pages/1.png",
      "uploadedAt": "2026-05-22T14:30:12"
    }
  ]
}
```

### 교안 상세

**GET** `/api/materials/{materialId}` | 🔒 동일 접근 정책

```json
{
  "success": true,
  "data": {
    "materialId": 1,
    "title": "1주차.pdf",
    "description": null,
    "pageCount": 12,
    "aspectRatio": "0.707",
    "createdBy": "교수닉네임",
    "createdAt": "2026-05-22T14:30:12"
  }
}
```

### 뷰어 메타데이터

**GET** `/api/materials/{materialId}/viewer` | 🔒 동일 접근 정책

```json
{
  "success": true,
  "data": {
    "materialId": 1,
    "pdfUrl": "https://.../1/uuid.pdf",
    "pageCount": 12,
    "aspectRatio": "0.707",
    "allowDownload": true
  }
}
```

### 페이지 이미지

**GET** `/api/materials/{materialId}/pages/{pageNumber}` | 🔒 동일 접근 정책

```json
{
  "success": true,
  "data": {
    "pageNumber": 1,
    "imageUrl": "https://.../pages/1.png"
  }
}
```

### 읽기 진행도 저장

**POST** `/api/materials/{materialId}/progress` | 🔒 동일 접근 정책

**Request**

```json
{ "currentPage": 12 }
```

**Response (200)**

```json
{
  "success": true,
  "message": "읽기 진행도가 저장되었습니다."
}
```

| 상황 | Status | errorCode | message |
|------|--------|-----------|---------|
| 접근 권한 없음 | `403` | `ACCESS_DENIED` | 해당 교안에 접근할 권한이 없습니다. |
| 교안 없음 | `404` | `MATERIAL_NOT_FOUND` | 존재하지 않는 교안입니다. |

---

## 14. 전체 교안 목록 조회 (관리자)

## 14. 전체 강의 목록 조회 (관리자)


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
  "message": "전체 강의 목록 조회 성공",
  "data": {
    "content": [ { "id": 5, "title": "...", "..." : "..." } ],
    "totalElements": 50,
    "totalPages": 5,
    "size": 10,
    "number": 0
  }
}
```

---

## 14-1. 교안 생성

**POST** `/api/lessons/{lessonId}/materials` | 🔒 PROF(강의 소유자)

> 강의에 교안을 추가한다. 해당 강의의 생성자(PROF)만 호출 가능.

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
    "id": 3,
    "lessonId": 5,
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

## 14-2. 교안 목록 조회

**GET** `/api/lessons/{lessonId}/materials` | 🔒 인증 필요

> - `PROF`/`ADMIN` — 해당 강의의 교안 전체 조회
> - `USER` — 해당 강의에 `APPROVED` 수강 신청이 있어야 교안 목록 조회 가능. 미승인 시 빈 페이지 반환.
>
> 기본값: `size=10`, `sort=createdAt,DESC`

### Response (200)

```json
{
  "status": 200,
  "message": "교안 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 3,
        "lessonId": 5,
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

## 14-3. 교안 단건 조회

**GET** `/api/lessons/{lessonId}/materials/{materialId}` | 🔒 인증 필요

> `USER`는 해당 강의에 `APPROVED` 수강 신청이 있어야 조회 가능. 미승인 시 `403`.

### Response (200)

```json
{
  "status": 200,
  "message": "교안 조회 성공",
  "data": {
    "id": 3,
    "lessonId": 5,
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

## 14-4. 교안 수정

**PUT** `/api/lessons/{lessonId}/materials/{materialId}` | 🔒 PROF(본인)/ADMIN

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
  "data": { "id": 3, "lessonId": 5, "title": "수정된 제목", "..." : "..." }
}
```

---

## 14-5. 교안 삭제

**DELETE** `/api/lessons/{lessonId}/materials/{materialId}` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{ "status": 200, "message": "교안 삭제 성공", "data": null }
```

---

## 15. 퀴즈 세트 생성

**POST** `/api/quiz` | 🔒 PROF

> 퀴즈는 반드시 한 교안에 속한다. `materialId`는 호출자(PROF)가 소유한 강의에 속한 교안 ID여야 한다.  
> `questions` 배열을 함께 전송하면 퀴즈와 문제가 하나의 트랜잭션으로 저장된다. 생략하면 빈 퀴즈로 생성된다.

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `title` | String | ✅ | 퀴즈 제목 |
| `description` | String | ❌ | 퀴즈 설명 |
| `materialId` | Long | ✅ | 퀴즈가 속할 교안 ID (요청자 소유 강의의 교안) |
| `questions` | Array | ❌ | 함께 생성할 문제 목록. 각 항목은 `POST /api/quiz/{quizId}/questions`의 Request Body와 동일 |

```json
{
  "title": "3장 운영체제 기초 퀴즈",
  "description": "3장 내용 복습용 퀴즈입니다.",
  "materialId": 3,
  "questions": [
    {
      "questionText": "프로세스와 스레드의 차이는?",
      "questionType": "SHORT_ANSWER",
      "correctAnswer": "스레드는 프로세스 내 실행 단위",
      "explanation": "스레드는 프로세스의 자원을 공유한다.",
      "score": 10,
      "options": []
    }
  ]
}
```

### Response (201)

응답은 생성된 퀴즈와 문제 목록을 포함한다 (`QuizDetailResponseDto`).

```json
{
  "status": 201,
  "message": "퀴즈 생성 성공",
  "data": {
    "id": 1,
    "title": "3장 운영체제 기초 퀴즈",
    "description": "3장 내용 복습용 퀴즈입니다.",
    "materialId": 3,
    "materialTitle": "3장. 프로세스와 스레드",
    "professorId": 10,
    "professorName": "김교수",
    "questions": [
      {
        "id": 1,
        "questionText": "프로세스와 스레드의 차이는?",
        "questionType": "SHORT_ANSWER",
        "score": 10,
        "options": []
      }
    ],
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| `materialId`가 존재하지 않음 | `404` | 존재하지 않는 교안입니다. |
| `materialId`가 본인 소유 강의 소속 아님 | `403` | 본인 소유의 강의에 속한 교안에만 퀴즈를 생성할 수 있습니다. |

---

## 16. 퀴즈 목록 조회

**GET** `/api/quiz` | 🔒 인증 필요

> 기본값: `size=10`, `sort=createdAt,DESC`
>
> 역할별 노출 범위:
> - `PROF` — 본인이 생성한 퀴즈만 조회
> - `USER` — 본인이 **APPROVED** 받은 강의에 속한 교안의 퀴즈만 조회 (미승인 강의 퀴즈 노출 없음)
> - `ADMIN` — 전체 퀴즈 조회
>
> `materialId` 파라미터를 지정하면 해당 교안의 퀴즈만 반환한다.
> USER가 `materialId`를 지정할 경우 해당 교안의 부모 강의가 APPROVED 상태가 아니면 빈 페이지를 반환한다.
>
> 소프트 삭제된 퀴즈는 모든 역할에서 노출되지 않는다.

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `materialId` | Long | ❌ | 특정 교안의 퀴즈만 필터링 |
| `page` | Integer | ❌ | 페이지 번호 (0부터, 기본값 0) |
| `size` | Integer | ❌ | 페이지 크기 (기본값 10) |
| `sort` | String | ❌ | 정렬 기준 (기본값 `createdAt,DESC`) |

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
        "materialId": 3,
        "materialTitle": "3장. 프로세스와 스레드",
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

> USER 역할은 해당 퀴즈가 속한 교안의 부모 강의에 대해 `APPROVED` 상태의 수강 신청이 있어야 호출 가능. 미승인 시 `403 ENROLLMENT_NOT_APPROVED`.

### Response (200)

```json
{
  "status": 200,
  "message": "퀴즈 조회 성공",
  "data": {
    "id": 1,
    "title": "3장 운영체제 기초 퀴즈",
    "description": "3장 내용 복습용 퀴즈입니다.",
    "materialId": 3,
    "materialTitle": "3장. 프로세스와 스레드",
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

## 18. 퀴즈 수정용 조회

**GET** `/api/quiz/{quizId}/edit` | 🔒 PROF(본인)/ADMIN

수정 화면에서 사용하는 preload 전용 엔드포인트. 17번(상세 조회)과 달리 각 문제의 `correctAnswer`, `explanation`, 각 옵션의 `correct` 플래그를 포함한다.

### Response (200)

```json
{
  "status": 200,
  "message": "퀴즈 수정용 조회 성공",
  "data": {
    "id": 1,
    "title": "3장 운영체제 기초 퀴즈",
    "description": "3장 내용 복습용 퀴즈입니다.",
    "materialId": 3,
    "materialTitle": "3장. 프로세스와 스레드",
    "professorId": 10,
    "professorName": "김교수",
    "questions": [
      {
        "id": 5,
        "questionText": "프로세스와 스레드의 차이점은?",
        "questionType": "MULTIPLE_CHOICE",
        "score": 10,
        "correctAnswer": "1",
        "explanation": "프로세스는 독립된 메모리 공간을 가진다.",
        "options": [
          { "id": 1, "optionText": "프로세스는 독립된 메모리 공간을 가진다.", "correct": true },
          { "id": 2, "optionText": "스레드는 서로 다른 힙을 사용한다.", "correct": false }
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

> 🔒 소유자(PROF) 또는 ADMIN만 호출 가능. 학생용 17번 엔드포인트와 응답 형태는 다르므로 프론트에서 수정 화면 진입 시에는 반드시 이 엔드포인트를 사용한다.

---

## 19. 퀴즈 수정

**PUT** `/api/quiz/{quizId}` | 🔒 PROF(본인)/ADMIN

> 1차 범위에서 `materialId`는 수정할 수 없다. 다른 교안으로 옮겨야 한다면 퀴즈를 삭제 후 재생성.

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

## 20. 퀴즈 삭제

**DELETE** `/api/quiz/{quizId}` | 🔒 PROF(본인)/ADMIN

> 소프트 삭제로 처리된다. `quiz` 및 하위 `quiz_q`, `quiz_opt` 에 `deleted=true`, `deleted_at=NOW()` 가 기록되고 실제 행은 유지된다. 삭제된 퀴즈는 이후 모든 조회 API 에서 노출되지 않는다.
>
> 학생 제출 이력(`quiz_submission`, `quiz_submission_answer`)은 그대로 보존되지만, 오답 조회 결과에서 소프트 삭제된 문제는 제외된다.

### Response (200)

```json
{ "status": 200, "message": "퀴즈 삭제 성공", "data": null }
```

---

## 21. 문제 추가

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

## 22. 문제 수정

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

## 23. 문제 삭제

**DELETE** `/api/quiz/{quizId}/questions/{questionId}` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{ "status": 200, "message": "문제 삭제 성공", "data": null }
```

---

## 24. 퀴즈 제출

**POST** `/api/quiz/{quizId}/submit` | 🔒 USER

> 퀴즈당 1회만 제출 가능합니다. 재제출 시 `409` 반환.
> 해당 퀴즈가 속한 교안에 `APPROVED` 상태 수강 신청이 있어야 호출 가능. 미승인 시 `403 ENROLLMENT_NOT_APPROVED`.

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

## 25. 오답 목록 조회

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
| `lessonRef.lessonId` | 참조 교안(lecture_material) ID |
| `lessonRef.lessonTitle` | 참조 교안 제목 |
| `lessonRef.lessonPage` | 교수가 지정한 교안 페이지 번호 |
| `lessonRef.lessonParagraph` | 교수가 지정한 교안 문단 번호 |

> `lessonRef`는 문제 생성 시 `anchorId`를 지정하지 않은 경우 `null`입니다.

---

## 26. 강의 수강 신청

**POST** `/api/lessons/{lessonId}/enrollments` | 🔒 USER

> 학생이 강의 수강을 신청한다. 신청은 `PENDING` 상태로 저장되며 교수의 수락이 있어야 해당 강의의 교안 및 퀴즈에 접근할 수 있다.

### Response (201)

```json
{
  "status": 201,
  "message": "수강 신청이 접수되었습니다.",
  "data": {
    "id": 42,
    "lessonId": 5,
    "lessonTitle": "운영체제 기초",
    "status": "PENDING",
    "requestedAt": "2026-05-21T09:10:00",
    "decidedAt": null
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 강의 없음 | `404` | 존재하지 않는 강의입니다. |
| 이미 신청한 강의 | `409` | 이미 신청한 강의입니다. |

---

## 27. 수강 신청 취소

**DELETE** `/api/lessons/{lessonId}/enrollments` | 🔒 USER

> 학생이 본인의 `PENDING` 신청을 취소한다. `APPROVED`/`REJECTED` 상태에서는 취소할 수 없다.

### Response (200)

```json
{ "status": 200, "message": "수강 신청이 취소되었습니다.", "data": null }
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 신청 없음 | `404` | 수강 신청을 찾을 수 없습니다. |
| 이미 결정된 신청 | `400` | 대기 중인 신청이 아닙니다. |

---

## 28. 학생 본인 승인 강의 목록

**GET** `/api/lessons/my` | 🔒 USER

> 학생 본인이 `APPROVED` 받은 강의 목록을 페이지네이션으로 반환한다. 메인 페이지의 "내 강의" 영역에 사용.

### Query Parameters (Pageable)

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `page` | Integer | 페이지 번호 (0부터, 기본값 0) |
| `size` | Integer | 페이지 크기 (기본값 10) |

### Response (200)

```json
{
  "status": 200,
  "message": "승인 강의 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 5,
        "title": "운영체제 기초",
        "description": "운영체제의 핵심 개념을 학습합니다.",
        "createdById": 10,
        "createdByName": "김교수",
        "approvedAt": "2026-05-20T10:30:00"
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

## 29. 강의 신청 목록 조회

**GET** `/api/lessons/{lessonId}/enrollments` | 🔒 PROF(본인)/ADMIN

> 교수가 본인 강의의 신청 목록을 조회한다. `status` 쿼리 파라미터로 필터링 가능.

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `status` | String | ❌ | `PENDING` / `APPROVED` / `REJECTED` (생략 시 전체) |
| `page` | Integer | ❌ | 페이지 번호 (기본 0) |
| `size` | Integer | ❌ | 페이지 크기 (기본 10) |

### Response (200)

```json
{
  "status": 200,
  "message": "수강 신청 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 42,
        "studentId": 7,
        "studentName": "이학생",
        "studentNickname": "minsik",
        "status": "PENDING",
        "requestedAt": "2026-05-21T09:10:00",
        "decidedAt": null
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

## 30. 수강 신청 수락

**POST** `/api/lessons/{lessonId}/enrollments/{enrollmentId}/approve` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{
  "status": 200,
  "message": "수강 신청을 수락했습니다.",
  "data": {
    "id": 42,
    "studentId": 7,
    "status": "APPROVED",
    "decidedAt": "2026-05-21T10:00:00"
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 신청 없음 | `404` | 수강 신청을 찾을 수 없습니다. |
| 이미 결정된 신청 | `400` | 대기 중인 신청이 아닙니다. |
| 권한 없음 | `403` | 접근 권한이 없습니다. |

---

## 31. 수강 신청 거절

**POST** `/api/lessons/{lessonId}/enrollments/{enrollmentId}/reject` | 🔒 PROF(본인)/ADMIN

### Response (200)

```json
{
  "status": 200,
  "message": "수강 신청을 거절했습니다.",
  "data": {
    "id": 42,
    "studentId": 7,
    "status": "REJECTED",
    "decidedAt": "2026-05-21T10:00:00"
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 신청 없음 | `404` | 수강 신청을 찾을 수 없습니다. |
| 이미 결정된 신청 | `400` | 대기 중인 신청이 아닙니다. |

---

---

## 32. 승인 대기 PROF 목록 조회

**GET** `/api/admin/professors/pending` | 🔒 ADMIN

> `status=PENDING`인 PROF 계정 목록을 페이지네이션으로 반환합니다.

### Query Parameters (Pageable)

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `page` | Integer | 페이지 번호 (0부터, 기본값 0) |
| `size` | Integer | 페이지 크기 (기본값 10) |
| `sort` | String | 정렬 기준 (기본값 `createdAt,ASC`) |

### Response (200)

```json
{
  "status": 200,
  "message": "승인 대기 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 5,
        "username": "김교수",
        "email": "prof@example.com",
        "nickname": "prof1",
        "status": "PENDING",
        "createdAt": "2026-05-24T10:00:00"
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

## 33. PROF 계정 승인

**POST** `/api/admin/professors/{id}/approve` | 🔒 ADMIN

> 지정한 사용자 계정을 `ACTIVE` 상태로 변경합니다.

### Path Variable

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 승인할 사용자 ID |

### Response (200)

```json
{
  "status": 200,
  "message": "계정을 승인했습니다.",
  "data": {
    "id": 5,
    "status": "ACTIVE"
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 사용자 없음 | `404` | 사용자를 찾을 수 없습니다. |

---

## 34. PROF 계정 거절

**POST** `/api/admin/professors/{id}/reject` | 🔒 ADMIN

> 지정한 사용자 계정을 `REJECTED` 상태로 변경합니다. DB 행은 유지되며 해당 이메일로 재가입이 차단됩니다.

### Path Variable

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 거절할 사용자 ID |

### Response (200)

```json
{
  "status": 200,
  "message": "계정을 거절했습니다.",
  "data": {
    "id": 5,
    "status": "REJECTED"
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 사용자 없음 | `404` | 사용자를 찾을 수 없습니다. |

---

## 35. PROF 계정 상태 임의 변경

**PATCH** `/api/admin/professors/{id}/status` | 🔒 ADMIN

> REJECTED → ACTIVE 등 후속 조정을 포함한 임의 상태 변경에 사용합니다.

### Path Variable

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 변경할 사용자 ID |

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `status` | String | ✅ | 변경할 상태 — `PENDING` / `ACTIVE` / `REJECTED` |

```json
{ "status": "ACTIVE" }
```

### Response (200)

```json
{
  "status": 200,
  "message": "계정 상태를 변경했습니다.",
  "data": {
    "id": 5,
    "status": "ACTIVE"
  }
}
```

| 상황 | Status | 메시지 |
|------|--------|--------|
| 사용자 없음 | `404` | 사용자를 찾을 수 없습니다. |
| 유효하지 않은 status 값 | `400` | 유효하지 않은 상태 값입니다. |

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
| `status` | VARCHAR(10) | `PENDING` / `ACTIVE` / `REJECTED`, NOT NULL, DEFAULT `ACTIVE` — PROF 승인 상태 |
| `provider` | VARCHAR(10) | `LOCAL` / `KAKAO` |
| `provider_id` | VARCHAR | 소셜 로그인 시 OIDC `sub`, LOCAL은 NULL |

> PROF 가입 시 `status=PENDING`으로 저장. USER·ADMIN 가입은 `status=ACTIVE`. 마이그레이션: `ALTER TABLE users ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'`

> 자세한 스키마는 [docs/generated/db-schema.md](generated/db-schema.md) 참조.

### lesson (강의)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `professor_id` | BIGINT | FK → users (PROF) |
| `title` | VARCHAR(255) | 강의 제목 |
| `description` | TEXT | 강의 설명 |
| `created_at` | TIMESTAMPTZ | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | 수정 일시 |

### lecture_material (교안)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `lesson_id` | BIGINT | FK → lesson, NOT NULL — 소속 강의 |
| `professor_id` | BIGINT | FK → users (PROF) |
| `title` | VARCHAR(255) | 교안 제목 |
| `content` | TEXT | 교안 설명 |
| `created_at` | TIMESTAMPTZ | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | 수정 일시 |

### lesson_enrollment (수강 신청)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `lesson_id` | BIGINT | FK → lesson, NOT NULL — 수강 신청 강의 |
| `student_id` | BIGINT | FK → users (USER), NOT NULL |
| `status` | VARCHAR(10) | `PENDING` / `APPROVED` / `REJECTED`, NOT NULL |
| `requested_at` | TIMESTAMPTZ | 신청 시각 |
| `decided_at` | TIMESTAMPTZ | 결정 시각 (nullable) |
| `decided_by` | BIGINT | FK → users (PROF/ADMIN, nullable) |

제약: `UNIQUE(lesson_id, student_id)` — 동일 학생-강의 조합 1행만 존재

### quiz (퀴즈 세트)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `professor_id` | BIGINT | FK → users |
| `lesson_id` | BIGINT | FK → lecture_material, NOT NULL — 퀴즈가 속한 교안 |
| `title` | VARCHAR(200) | 퀴즈 제목 |
| `description` | VARCHAR(500) | 퀴즈 설명 |

### quiz_q (퀴즈 문제)

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | PK |
| `quiz_id` | BIGINT | FK → quiz |
| `anchor_id` | BIGINT | FK → lecture_material (NULL 가능) — 참조 교안 |
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
