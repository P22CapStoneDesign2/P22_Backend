# PRD — EQZ 서비스 기획서

## 1. 서비스 개요

교수(강사)와 학생을 위한 교안 관리 및 퀴즈(문제 은행) 플랫폼.

- **기술 스택**: Java Spring Boot, OpenJDK 21
- **Base URL**: `https://api.example.com`
- **인증 방식**: JWT Bearer Token (Access Token + Refresh Token)

---

## 2. 권한 정의

| Role | 설명 |
|------|------|
| `PROF` | 강사 (교수) |
| `USER` | 학생 |
| `ADMIN` | 서비스 관리자 |

---

## 3. 권한별 사용 시나리오

### 강사 (PROF)
- 교안 생성, 수정, 삭제
- 퀴즈 생성, 수정, 삭제
- 퀴즈 답안지 열람
- 퀴즈 통계 보기

### 학생 (USER)
- 교안 보기
- 퀴즈 보기
- 퀴즈 답안지 작성

### 서비스 관리자 (ADMIN)
- 교안 데이터베이스 관리
- 퀴즈 데이터베이스 관리

---

## 4. 엔드포인트 접근 제어 (RBAC)

```java
.requestMatchers("/api/auth/**").permitAll()        // 누구나
.requestMatchers("/api/quiz/create").hasRole("PROF") // 교수만
.requestMatchers("/api/admin/**").hasRole("ADMIN")   // 관리자만
```

---

## 5. 도메인 구성

| 도메인 | 경로 | 주요 기능 |
|--------|------|-----------|
| user | `/api/auth/**`, `/api/users/**` | 인증, 회원가입, 프로필 관리 |
| quiz | `/api/quiz/**` | 문제 은행 관리 |
| lesson | `/api/lesson/**` | 교안 뷰어 |
