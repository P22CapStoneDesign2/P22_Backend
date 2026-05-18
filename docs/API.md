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

### 인증 / 사용자

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
| `GET` | `/api/quiz/{quizId}` | ✅ | 모두 | 퀴즈 상세 조회 (문제 포함, 정답 미노출) |
| `GET` | `/api/quiz/{quizId}/edit` | ✅ | PROF(본인)/ADMIN | 퀴즈 수정용 조회 (정답·해설 포함, preload) |
| `PUT` | `/api/quiz/{quizId}` | ✅ | PROF(본인)/ADMIN | 퀴즈 수정 |
| `DELETE` | `/api/quiz/{quizId}` | ✅ | PROF(본인)/ADMIN | 퀴즈 삭제 |
| `POST` | `/api/quiz/{quizId}/questions` | ✅ | PROF(본인)/ADMIN | 문제 추가 |
| `PUT` | `/api/quiz/{quizId}/questions/{questionId}` | ✅ | PROF(본인)/ADMIN | 문제 수정 |
| `DELETE` | `/api/quiz/{quizId}/questions/{questionId}` | ✅ | PROF(본인)/ADMIN | 문제 삭제 |
| `POST` | `/api/quiz/{quizId}/submit` | ✅ | USER | 퀴즈 제출 |
| `GET` | `/api/quiz/wrong-answers` | ✅ | USER | 오답 목록 조회 (페이지네이션) |

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

최종 콜백: `https://프론트엔드주소/oauth2/callback?accessToken=...&refreshToken=...`

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

**POST** `/api/auth/logout` | 🔒 인증 필요

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
    "role": "STUDENT",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

| 필드 | 설명 |
|------|------|
| `provider` | `LOCAL` / `KAKAO` |
| `role` | `STUDENT` / `PROFESSOR` / `ADMIN` |

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
    "role": "STUDENT",
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
| `username` | VARCHAR(50) | 닉네임 |
| `email` | VARCHAR(100) | 이메일 (소셜 유저는 플레이스홀더) |
| `password` | VARCHAR(255) | NULL (소셜 로그인) |
| `role` | user_role | `STUDENT` / `PROFESSOR` / `ADMIN` |
| `provider` | VARCHAR(10) | `LOCAL` / `KAKAO` |

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
