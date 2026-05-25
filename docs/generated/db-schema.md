# DB 스키마 (자동 갱신 대상)

> 이 파일은 엔티티 클래스 기반으로 유지됩니다.
> 엔티티 변경 시 이 파일도 함께 업데이트해야 합니다.
> 마지막 갱신: 2026-05-25 (quiz.lesson_id → quiz.lesson_material_id 리네임)

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
| `status` | VARCHAR(10) | NOT NULL, DEFAULT `'ACTIVE'` | `PENDING` \| `ACTIVE` \| `REJECTED` — PROF 승인 상태 |
| `deleted` | BOOLEAN | NOT NULL, DEFAULT false | Soft Delete 여부 |
| `deleted_at` | TIMESTAMP | NULL | 탈퇴 일시 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

> `UserStatus` enum 은 `domain/user/enums/UserStatus.java` 에 정의. PROF 가입 시 `PENDING`으로 저장되고 ADMIN 승인 후 `ACTIVE`, 거절 시 `REJECTED` 로 변경된다. USER·ADMIN은 항상 `ACTIVE`. 기존 행은 컬럼 추가 시 PostgreSQL DEFAULT 제약으로 `ACTIVE` 백필.

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

## lesson (강의)

엔티티: `domain/lesson/entity/Lesson.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `title` | VARCHAR(255) | NOT NULL | 강의 제목 |
| `description` | TEXT | NULL | 강의 설명 |
| `professor_id` | BIGINT | NULL, FK → users | 강의 생성 교수 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

---

## lecture_material (교안)

엔티티: `domain/lesson/entity/LessonMaterial.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `lesson_id` | BIGINT | NOT NULL, FK → lesson | 소속 강의 |
| `title` | VARCHAR(255) | NOT NULL | 교안 제목 |
| `content` | TEXT | NULL | 교안 설명 (`description` 필드) |
| `professor_id` | BIGINT | NULL, FK → users | 교안 작성 교수 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

---

## lesson_enrollment (수강 신청)

엔티티: `domain/lesson/entity/LessonEnrollment.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `lesson_id` | BIGINT | NOT NULL, FK → lesson | 신청 대상 강의 |
| `student_id` | BIGINT | NOT NULL, FK → users (USER) | 신청 학생 |
| `status` | VARCHAR(10) | NOT NULL | `PENDING` \| `APPROVED` \| `REJECTED` |
| `requested_at` | TIMESTAMP | NOT NULL | 신청 시각 |
| `decided_at` | TIMESTAMP | NULL | 결정 시각 (APPROVED/REJECTED 시 기록) |
| `decided_by` | BIGINT | NULL, FK → users (PROF/ADMIN) | 결정자 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| UNIQUE | — | (`lesson_id`, `student_id`) | 학생-강의 1:1 |

> `EnrollmentStatus` enum 은 `domain/lesson/enums/EnrollmentStatus.java` 에 정의.

---

## quiz (퀴즈 세트)

엔티티: `domain/quiz/entity/Quiz.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `professor_id` | BIGINT | NOT NULL, FK → users | 출제 교수 |
| `lesson_material_id` | BIGINT | NOT NULL, FK → lecture_material | 퀴즈가 속한 교안 (게이팅 기준) |
| `title` | VARCHAR(200) | NOT NULL | 퀴즈 제목 |
| `description` | VARCHAR(500) | NULL | 퀴즈 설명 |
| `deleted` | BOOLEAN | NOT NULL, DEFAULT false | 소프트 삭제 플래그 |
| `deleted_at` | TIMESTAMP | NULL | 소프트 삭제 시각 |
| `created_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP | NOT NULL | BaseTimeEntity |

> 1차 범위에서 `lesson_material_id`는 생성 시점 고정, `PUT /api/quiz/{quizId}`에서는 변경 불가.

> Hibernate `@SQLDelete` / `@SQLRestriction` 으로 자동 소프트 삭제 및 조회 제외 적용. `JpaRepository.delete()` 호출 시 `UPDATE quiz SET deleted=true, deleted_at=NOW()` 가 실행되며, 모든 JPA 조회에서 `deleted=false` 필터가 자동 부착된다.

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
| `deleted` | BOOLEAN | NOT NULL, DEFAULT false | 소프트 삭제 플래그 |
| `deleted_at` | TIMESTAMP | NULL | 소프트 삭제 시각 |

> 부모 `quiz` 가 소프트 삭제될 때 cascade 로 같이 마킹된다. `@SQLRestriction` 으로 모든 조회에서 자동 제외.

---

## quiz_opt (객관식 보기)

엔티티: `domain/quiz/entity/QuizOption.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `question_id` | BIGINT | NOT NULL, FK → quiz_q | 소속 문제 |
| `option_text` | VARCHAR(500) | NOT NULL | 보기 내용 |
| `is_correct` | BOOLEAN | NOT NULL | 정답 여부 |
| `deleted` | BOOLEAN | NOT NULL, DEFAULT false | 소프트 삭제 플래그 |
| `deleted_at` | TIMESTAMP | NULL | 소프트 삭제 시각 |

> `@SQLDelete` 만 적용 (`@SQLRestriction` 없음) — 옵션은 question 통해서만 조회되므로 question 차원에서 이미 가려진다. 데이터 손실 방지 목적.

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
users (1) ──── (N) lesson                 ← professor_id
users (1) ──── (N) lecture_material       ← professor_id
users (1) ──── (N) quiz
users (1) ──── (N) quiz_sub
users (1) ──── (N) lesson_enrollment      ← student_id
users (1) ──── (N) lesson_enrollment      ← decided_by (nullable)

lesson (1) ──── (N) lecture_material      ← lesson_id (NOT NULL)
lesson (1) ──── (N) lesson_enrollment     ← lesson_id (NOT NULL)

lecture_material (1) ──── (N) quiz        ← lesson_material_id (NOT NULL)

quiz (1) ──── (N) quiz_q
quiz (1) ──── (N) quiz_sub

quiz_q (1) ──── (N) quiz_opt
quiz_q (N) ──── (1) lecture_material  ← anchor_id (nullable)

quiz_sub (1) ──── (N) quiz_sub_answer
quiz_sub_answer (N) ──── (1) quiz_q
```
