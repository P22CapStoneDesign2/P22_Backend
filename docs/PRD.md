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
- 퀴즈 세트 생성, 수정, 삭제
- 퀴즈 문제 추가·수정·삭제 (교안 페이지/문단 참조 지정 포함)
- 퀴즈 통계 보기

### 학생 (USER)
- 교안 보기
- 퀴즈 풀기 (답안 제출)
- 오답 정리 보기 (교수가 지정한 교안 페이지/문단 참조 포함)

### 서비스 관리자 (ADMIN)
- 교안 데이터베이스 관리
- 퀴즈 데이터베이스 관리

---

## 4. 엔드포인트 접근 제어 (RBAC)

```java
.requestMatchers("/api/auth/**").permitAll()               // 누구나
.requestMatchers(GET, "/api/lessons/**").hasAnyRole("USER","PROF","ADMIN")
.requestMatchers("/api/lessons/**").hasAnyRole("PROF","ADMIN")
.requestMatchers("/api/quiz/**").authenticated()           // 인증 후 메서드 레벨 @PreAuthorize로 세분화
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

---

## 5. 도메인 구성

| 도메인 | 경로 | 주요 기능 |
|--------|------|-----------|
| user | `/api/auth/**`, `/api/users/**` | 인증, 회원가입, 프로필 관리 |
| quiz | `/api/quiz/**` | 퀴즈 세트·문제 관리, 답안 제출, 오답 조회 |
| lesson | `/api/lessons/**` | 교안 뷰어 |

---

## 6. 퀴즈-교안 연동 기능

### 개요

교수가 퀴즈 문제를 생성할 때 해당 문제가 **어떤 교안의 몇 페이지, 몇 번째 문단**에서 왔는지 지정한다.
학생이 퀴즈를 제출한 후 오답 정리를 할 때, 틀린 문제마다 교수가 지정한 **교안 참조 정보(교안명, 페이지, 문단)**를 함께 확인할 수 있다.

### 데이터 흐름

```
① 교수 → 문제 생성 시 anchorId (교안 ID), lessonPage, lessonParagraph 지정
② 학생 → 퀴즈 제출 (POST /api/quiz/{quizId}/submit)
③ 서버 → 자동 채점 후 quiz_sub / quiz_sub_answer 저장
④ 학생 → 오답 목록 조회 (GET /api/quiz/wrong-answers)
⑤ 응답 → 틀린 문제 + lessonRef { lessonId, lessonTitle, lessonPage, lessonParagraph }
```

### DB 연관 구조

```
quiz (퀴즈 세트)
 └─ quiz_q (문제)  ──anchor_id──▶ lecture_material (교안)
      ├─ lesson_page      (지정 페이지)
      ├─ lesson_paragraph (지정 문단)
      └─ quiz_opt (객관식 보기)

quiz_sub (제출)
 └─ quiz_sub_answer (문제별 답안)  ──question_id──▶ quiz_q
```
