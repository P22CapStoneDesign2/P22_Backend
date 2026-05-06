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
| 401 | 인증 실패 (토큰 없음 / 만료) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 데이터 |
| 500 | 서버 내부 오류 |

---

## 엔드포인트 요약

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/auth/signup` | ❌ | 회원가입 |
| `POST` | `/api/auth/login` | ❌ | 일반 로그인 |
| `GET` | `/oauth2/authorization/kakao` | ❌ | Kakao 소셜 로그인 |
| `POST` | `/api/auth/reissue` | ❌ | Access Token 재발급 |
| `POST` | `/api/auth/logout` | ✅ | 로그아웃 |
| `GET` | `/api/users/me` | ✅ | 회원 정보 조회 |
| `PATCH` | `/api/users/me` | ✅ | 회원 정보 수정 |
| `DELETE` | `/api/users/me` | ✅ | 회원 탈퇴 |

---

## 1. 회원가입

**POST** `/api/auth/signup`

### Request Body

| 파라미터 | 타입 | 필수 | 설명 | 유효성 |
|----------|------|------|------|--------|
| `username` | String | ✅ | 닉네임 | 2~20자 |
| `email` | String | ✅ | 이메일 | 이메일 형식, 중복 불가 |
| `password` | String | ✅ | 비밀번호 | 8~20자, 영문+숫자+특수문자 |
| `passwordConfirm` | String | ✅ | 비밀번호 확인 | password와 일치 |

```json
{
  "username": "홍길동",
  "email": "hong@example.com",
  "password": "Test1234!",
  "passwordConfirm": "Test1234!"
}
```

### Response

| 상황 | Status | 메시지 |
|------|--------|--------|
| 성공 | `201` | 회원가입 성공 |
| 이메일 중복 | `409` | 이미 사용 중인 이메일입니다. |
| 유효성 실패 | `400` | 비밀번호 형식이 올바르지 않습니다. |

---

## 2. 일반 로그인

**POST** `/api/auth/login`

### Request Body

```json
{
  "email": "hong@example.com",
  "password": "Test1234!"
}
```

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

> **인증 방식**: Kakao OpenID Connect (OIDC)
> 이메일 스코프가 필요 없으며 비즈앱 전환 없이 사용 가능합니다.
> 유저 식별 기준은 **카카오 회원번호 (`sub` claim)** 입니다.

### 3-1. 로그인 요청

**GET** `/oauth2/authorization/kakao` → Kakao OIDC 인증 페이지로 리다이렉트

### 3-2. 처리 플로우

```
① 클라이언트 → GET /oauth2/authorization/kakao
② 서버 → Kakao OIDC 인증 페이지로 리다이렉트 (scope: openid profile_nickname)
③ 사용자 → Kakao 계정 선택 및 동의
④ Kakao → 서버 콜백 (/login/oauth2/code/kakao) + ID Token (sub, nickname)
⑤ 서버 → sub 기준으로 신규 사용자이면 자동 회원가입, 기존이면 조회
⑥ 서버 → JWT 발급 후 프론트엔드로 리다이렉트
```

### 3-3. 유저 식별 방식

| 항목 | 내용 |
|------|------|
| 식별 기준 | `provider` + `providerId` (카카오 `sub` claim) |
| 이메일 | 사용하지 않음 — DB에는 `kakao_{sub}@social.user` 형식의 플레이스홀더 저장 |
| 닉네임 | OIDC `nickname` claim 사용 (없으면 `카카오유저`로 대체) |

### 3-4. 최종 콜백

```
https://프론트엔드주소/oauth2/callback?accessToken=...&refreshToken=...
```

| 상황 | 처리 방식 |
|------|-----------|
| 신규 소셜 사용자 | 자동 회원가입 후 JWT 발급 |
| 기존 소셜 사용자 | `providerId` 조회 후 JWT 발급 |

---

## 4. 토큰 재발급

**POST** `/api/auth/reissue`

### Request Body

```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

### Response (200)

```json
{
  "status": 200,
  "message": "토큰 재발급 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

> Refresh Token Rotation 적용 — 재발급 시 기존 토큰 폐기

---

## 5. 로그아웃

**POST** `/api/auth/logout` | 🔒 인증 필요

**Header**: `Authorization: Bearer {accessToken}`

### Request Body

```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

> 서버에서 Refresh Token을 DB에서 삭제 처리

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

| 필드 | 타입 | 설명 |
|------|------|------|
| `provider` | String | `LOCAL` / `KAKAO` |
| `providerId` | String | 소셜 사용자의 카카오 `sub` 값. LOCAL은 `null` |
| `role` | String | `USER` / `PROF` / `ADMIN` |

---

## 7. 회원 정보 수정

**PATCH** `/api/users/me` | 🔒 인증 필요

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `username` | String | ❌ | 변경할 닉네임 (2~20자) |
| `currentPassword` | String | ❌ | 현재 비밀번호 (비번 변경 시 필수) |
| `newPassword` | String | ❌ | 변경할 비밀번호 (8~20자) |

> ⚠️ 소셜 로그인 사용자는 비밀번호 변경 불가

---

## 8. 회원 탈퇴

**DELETE** `/api/users/me` | 🔒 인증 필요

### Request Body

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `password` | String | ❌ | 일반 로그인 사용자만 필요 |

---

## DB 테이블 설계

### users

| 컬럼명 | 타입 | 제약 조건 | 설명 |
|--------|------|-----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 사용자 고유 ID |
| `username` | VARCHAR(20) | NOT NULL | 닉네임 |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE | 이메일 (소셜 유저는 `kakao_{sub}@social.user` 플레이스홀더) |
| `password` | VARCHAR(255) | NULL | 소셜 로그인은 NULL |
| `provider` | VARCHAR(10) | NOT NULL | `LOCAL` / `KAKAO` |
| `provider_id` | VARCHAR(255) | NULL | 소셜 로그인 제공자의 고유 ID (`sub`). LOCAL은 NULL |
| `role` | VARCHAR(10) | NOT NULL, DEFAULT 'USER' | 권한 |
| `deleted` | BOOLEAN | NOT NULL, DEFAULT false | 소프트 삭제 여부 |
| `deleted_at` | DATETIME | NULL | 탈퇴 처리 일시 |
| `created_at` | DATETIME | NOT NULL | 가입일시 |
| `updated_at` | DATETIME | NOT NULL | 수정일시 |

### refresh_tokens

| 컬럼명 | 타입 | 제약 조건 | 설명 |
|--------|------|-----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 고유 ID |
| `user_id` | BIGINT | NOT NULL | 사용자 ID |
| `token` | VARCHAR(512) | NOT NULL, UNIQUE | Refresh Token 값 |
| `expiry_date` | DATETIME | NOT NULL | 만료일시 |
| `created_at` | DATETIME | NOT NULL | 발급일시 |
| `updated_at` | DATETIME | NOT NULL | 갱신일시 |
