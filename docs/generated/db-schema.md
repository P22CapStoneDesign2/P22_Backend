# DB 스키마 (자동 갱신 대상)

> 이 파일은 엔티티 클래스 기반으로 유지됩니다.
> 엔티티 변경 시 이 파일도 함께 업데이트해야 합니다.
> 마지막 갱신: 2026-05-14

---

## users

엔티티: `domain/user/entity/User.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `username` | VARCHAR(20) | NOT NULL | 이름 |
| `nickname` | VARCHAR(20) | NOT NULL, UNIQUE | 닉네임 (영문·숫자·한글, 2~20자) |
| `password` | VARCHAR | NULL | LOCAL 계정만 사용, 소셜 로그인은 NULL |
| `email` | VARCHAR | NOT NULL, UNIQUE | |
| `provider` | VARCHAR(10) | NOT NULL | `LOCAL` \| `KAKAO` |
| `provider_id` | VARCHAR | NULL | 소셜 로그인 제공자 식별자 (Kakao OIDC `sub`) |
| `role` | VARCHAR(10) | NOT NULL | `PROF` \| `USER` \| `ADMIN` |
| `deleted` | BOOLEAN | NOT NULL, DEFAULT false | Soft Delete 여부 |
| `deleted_at` | TIMESTAMP | NULL | 탈퇴 일시 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

---

## refresh_tokens

엔티티: `domain/user/entity/RefreshToken.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `token` | VARCHAR | NOT NULL, UNIQUE | Refresh Token 값 |
| `user_id` | BIGINT | NOT NULL | users.id 참조 (FK 미설정) |
| `expires_at` | TIMESTAMP | NOT NULL | 만료 일시 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

---

## lecture_material (교안)

엔티티: `domain/lesson/entity/Lesson.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `title` | VARCHAR(255) | NOT NULL | 교안 제목 |
| `content` | TEXT | NULL | 교안 설명 (`description` 필드) |
| `professor_id` | BIGINT | NULL, FK → users | 작성자 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

---

## quiz (퀴즈 세트)

엔티티: `domain/quiz/entity/Quiz.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `professor_id` | BIGINT | NOT NULL, FK → users | 출제 교수 |
| `title` | VARCHAR(200) | NOT NULL | 퀴즈 제목 |
| `description` | VARCHAR(500) | NULL | 퀴즈 설명 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

---

## quiz_q (퀴즈 문제)

엔티티: `domain/quiz/entity/QuizQuestion.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `quiz_id` | BIGINT | NOT NULL, FK → quiz | 소속 퀴즈 |
| `anchor_id` | BIGINT | NULL, FK → lecture_material | 참조 교안 |
| `question_text` | TEXT | NOT NULL | 문제 내용 |
| `q_type` | VARCHAR(20) | NOT NULL | `MULTIPLE_CHOICE` \| `SHORT_ANSWER` |
| `score` | INT | NOT NULL | 배점 |
| `correct_answer` | TEXT | NULL | 정답 (대소문자 무시 비교) |
| `explanation` | TEXT | NULL | 해설 |
| `lesson_page` | INT | NULL | 교수 지정 교안 페이지 |
| `lesson_paragraph` | INT | NULL | 교수 지정 교안 문단 |

---

## quiz_opt (객관식 보기)

엔티티: `domain/quiz/entity/QuizOption.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `question_id` | BIGINT | NOT NULL, FK → quiz_q | 소속 문제 |
| `option_text` | VARCHAR(500) | NOT NULL | 보기 내용 |
| `is_correct` | BOOLEAN | NOT NULL | 정답 여부 |

---

## quiz_sub (퀴즈 제출)

엔티티: `domain/quiz/entity/QuizSubmission.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `quiz_id` | BIGINT | NOT NULL, FK → quiz | |
| `student_id` | BIGINT | NOT NULL, FK → users | |
| `total_score` | INT | NOT NULL | 총점 |
| `submitted_at` | TIMESTAMP | NOT NULL | 제출 일시 (자동 설정) |
| UNIQUE | — | (`quiz_id`, `student_id`) | 재제출 방지 |

---

## quiz_sub_answer (문제별 답안)

엔티티: `domain/quiz/entity/QuizSubmissionAnswer.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `submission_id` | BIGINT | NOT NULL, FK → quiz_sub | |
| `question_id` | BIGINT | NOT NULL, FK → quiz_q | |
| `student_answer` | VARCHAR(500) | NULL | 학생 답안 |
| `is_correct` | BOOLEAN | NOT NULL | 채점 결과 |
| `score` | INT | NOT NULL | 획득 점수 |

---

## ERD 요약

```
users (1) ──── (N) lecture_material
users (1) ──── (N) quiz
users (1) ──── (N) quiz_sub

quiz (1) ──── (N) quiz_q
quiz (1) ──── (N) quiz_sub

quiz_q (1) ──── (N) quiz_opt
quiz_q (N) ──── (1) lecture_material   ← anchor_id (nullable)

quiz_sub (1) ──── (N) quiz_sub_answer
quiz_sub_answer (N) ──── (1) quiz_q
```
