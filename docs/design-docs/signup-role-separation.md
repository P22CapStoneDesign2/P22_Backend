# 설계: PROF / USER 회원가입 분리

- **상태**: 설계 중
- **관련 exec-plan**: `exec-plans/active/signup-role-separation.md`
- **대상 도메인**: user, global.oauth2, global.jwt

---

## 1. API 명세 초안

### 삭제

| 메서드 | 경로 | 이유 |
|--------|------|------|
| POST | `/api/auth/signup` | `profsignup`으로 대체 |

---

### 신규 / 변경

#### POST /api/auth/profsignup
교수 로컬 회원가입. 성공 시 Role.PROF로 저장.

**Request Body**
```json
{
  "username": "홍길동",
  "email": "hong@university.ac.kr",
  "nickname": "gildong",
  "password": "Password1!",
  "passwordConfirm": "Password1!"
}
```

**유효성 규칙**
| 필드 | 규칙 |
|------|------|
| `username` | 필수, 2~20자 |
| `email` | 필수, 이메일 형식, DB unique |
| `nickname` | 필수, 영문·숫자·한글, 2~20자, DB unique |
| `password` | 필수, 8~20자, 영문+숫자+특수문자 포함 |
| `passwordConfirm` | 필수, password와 일치 |

**Response**
```json
// 201 Created
{ "status": 201, "message": "회원가입 성공", "data": null }

// 400 PASSWORD_CONFIRM_MISMATCH
// 409 EMAIL_ALREADY_EXISTS
// 409 NICKNAME_ALREADY_EXISTS
```

---

#### POST /api/auth/usersignup
카카오 신규 유저 가입 완료. pending 토큰 검증 후 Role.USER로 저장 + JWT 발급.

**Request Body**
```json
{
  "pendingToken": "<10분짜리 JWT>",
  "username": "김학생",
  "email": "student@gmail.com",
  "nickname": "studyking"
}
```

**유효성 규칙**
| 필드 | 규칙 |
|------|------|
| `pendingToken` | 필수 |
| `username` | 필수, 2~20자 |
| `email` | 필수, 이메일 형식, DB unique |
| `nickname` | 필수, 영문·숫자·한글, 2~20자, DB unique |

**Response**
```json
// 201 Created
{
  "status": 201,
  "message": "회원가입 성공",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "tokenType": "Bearer"
  }
}

// 401 INVALID_PENDING_TOKEN  — 만료·위변조된 pending 토큰
// 409 EMAIL_ALREADY_EXISTS
// 409 NICKNAME_ALREADY_EXISTS
```

---

#### GET /api/auth/check-nickname?nickname={nickname}
닉네임 중복 확인. 인증 불필요(public).

**Response**
```json
// 200 OK — 사용 가능
{ "status": 200, "message": "사용 가능한 닉네임입니다.", "data": { "available": true } }

// 200 OK — 중복
{ "status": 200, "message": "이미 사용 중인 닉네임입니다.", "data": { "available": false } }
```

> 중복 여부를 에러가 아닌 200 + `available` 필드로 반환하는 이유:
> 프론트에서 입력 중 실시간 호출하므로 에러 처리 없이 단순 분기가 가능해야 함.

---

## 2. OAuth2 흐름 변경

### 기존 유저 (변경 없음)
```
카카오 인증 완료 → OAuth2SuccessHandler → JWT 발급
→ {redirect-uri}?accessToken=xxx&refreshToken=xxx
```

### 신규 유저 (변경)
```
카카오 인증 완료 → OAuth2SuccessHandler → pending 토큰 생성
→ {register-uri}?pendingToken=xxx&kakaoName=홍길동
  (프론트: 이름 필드에 kakaoName pre-fill, 수정 가능)
→ 유저가 이름·이메일·닉네임 입력 후 제출
→ POST /api/auth/usersignup
→ Role.USER 저장 + JWT 발급
→ {redirect-uri}?accessToken=xxx&refreshToken=xxx
```

**pending 토큰 클레임**
| 클레임 | 값 |
|--------|----|
| `sub` | providerId (카카오 고유 ID) |
| `type` | `PENDING_SOCIAL` |
| `provider` | `KAKAO` |
| `name` | 카카오 profile_nickname (pre-fill용) |
| 유효기간 | 10분 |

---

## 3. 설정 변경 (application-local.yaml)

```yaml
app:
  oauth2:
    redirect-uri: http://localhost:5174/oauth2/callback   # 기존
    register-uri: http://localhost:5174/oauth2/register   # 신규 — 학생 정보 입력 페이지
```

---

## 4. 엔티티 변경 없음

Role 컬럼은 이미 존재함. 기존 `createSocialUser()`에서 `Role.USER`를 명시하고 있었고,
`signup()`에서 Role을 설정하지 않아 기본값(USER)이 들어갔던 버그를 이번에 함께 수정함
→ `profSignup()`에서 `Role.PROF` 명시.
