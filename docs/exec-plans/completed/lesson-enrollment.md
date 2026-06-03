# [완료] 교안 수강 신청 및 컨텐츠 접근 게이팅

- **시작일**: 2026-05-21
- **완료일**: 2026-05-22
- **브랜치**: feat/lesson_sik

## 목표

학생(USER)이 교안 수강을 신청하고 교수(PROF)가 이를 수락하는 흐름을 도입한다.
- `GET /api/lessons` 목록은 USER에게 신청 가능한 전체 교안을 그대로 노출한다 (메인 페이지 용도).
- 교안 하위 컨텐츠(퀴즈, 추후 PDF 등)는 APPROVED 받은 학생만 접근할 수 있다.

## 수용 기준

- [x] `LessonEnrollment` 엔티티가 `domain/lesson/entity/`에 추가되고 ArchUnit이 통과한다
- [x] `POST /api/lessons/{id}/enrollments` — 학생 수강 신청 (PENDING 저장)
- [x] 중복 신청은 409 `ENROLLMENT_DUPLICATE`
- [x] `DELETE /api/lessons/{id}/enrollments` — 본인 신청 취소 (PENDING일 때만)
- [x] `GET /api/lessons/{id}/enrollments?status=PENDING|APPROVED|REJECTED` — 교수가 본인 교안 신청 목록 조회
- [x] `POST /api/lessons/{id}/enrollments/{enrollmentId}/approve` — 교수 수락
- [x] `POST /api/lessons/{id}/enrollments/{enrollmentId}/reject` — 교수 거절
- [x] `GET /api/lessons/my` — 학생이 APPROVED 받은 교안 목록
- [x] USER가 미승인 교안의 퀴즈를 조회/제출하려 하면 403 `ENROLLMENT_NOT_APPROVED`
- [x] `GET /api/quiz` — USER 호출 시 APPROVED 교안 퀴즈만 자동 스코핑 (미승인 교안 퀴즈 목록 노출 없음)
- [x] `GET /api/quiz?lessonId={id}` — 교안 필터링 지원. USER는 해당 교안이 미승인이면 빈 페이지 반환
- [x] `Quiz.lessonId` (NOT NULL FK) 추가, `POST /api/quiz` 요청에 `lessonId` 필수, 응답에 `lessonId`/`lessonTitle` 포함
- [x] `POST /api/quiz` 시 교수가 본인 소유 아닌 교안을 지정하면 403
- [x] `GET /api/lessons` 목록 응답은 USER에게 그대로 노출 (변경 없음)
- [x] `GET /api/lessons/{id}` 단건 메타 정보는 인증된 모두 조회 가능 (게이팅 없음)
- [x] `docs/API.md`, `docs/ARCHITECTURE.md`, `docs/generated/db-schema.md` 갱신
- [x] 서비스 단위 테스트, 통합 테스트 추가
- [x] `./gradlew build` 통과 (ArchUnit 포함)

## API 추가/변경

### 신규

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/api/lessons/{id}/enrollments` | USER | 수강 신청 |
| DELETE | `/api/lessons/{id}/enrollments` | USER | 본인 신청 취소 (PENDING일 때만) |
| GET | `/api/lessons/my` | USER | 내가 APPROVED 받은 교안 목록 (페이지네이션) |
| GET | `/api/lessons/{id}/enrollments` | PROF(본인)/ADMIN | 신청 목록 (status 쿼리) |
| POST | `/api/lessons/{id}/enrollments/{enrollmentId}/approve` | PROF(본인)/ADMIN | 수락 |
| POST | `/api/lessons/{id}/enrollments/{enrollmentId}/reject` | PROF(본인)/ADMIN | 거절 |

### 변경 (게이팅 추가 + 모델 변경)

- `POST /api/quiz` — 요청 본문에 `lessonId`(Long, 필수) 추가. 교수가 본인 소유 교안만 지정 가능. 응답 DTO에도 `lessonId`, `lessonTitle` 포함.
- `GET /api/quiz`, `GET /api/quiz/{quizId}`, `GET /api/quiz/{quizId}/edit` — 응답에 `lessonId`, `lessonTitle` 포함
- `GET /api/quiz` — USER: APPROVED 교안 퀴즈만 자동 스코핑. `?lessonId` 쿼리 파라미터 추가 (전체 역할). USER+lessonId 지정 시 미승인이면 빈 페이지 반환.
- `PUT /api/quiz/{quizId}` — `lessonId` 변경 가능 여부는 1차 범위에서 제외 (수정 불가로 시작)
- `GET /api/quiz/{quizId}` — USER 호출 시 `quiz.lessonId`로 enrollment APPROVED 검사
- `POST /api/quiz/{quizId}/submit` — 동일 검사
- `GET /api/quiz/wrong-answers` — 변경 없음 (이미 본인 데이터)

## ErrorCode 추가

| 코드 | Status | 메시지 |
|------|--------|--------|
| `ENROLLMENT_DUPLICATE` | 409 | 이미 신청한 교안입니다. |
| `ENROLLMENT_NOT_FOUND` | 404 | 수강 신청을 찾을 수 없습니다. |
| `ENROLLMENT_NOT_APPROVED` | 403 | 수강 승인되지 않은 교안입니다. |
| `ENROLLMENT_NOT_PENDING` | 400 | 대기 중인 신청이 아닙니다. |

## DB 변경

### lesson_enrollment (신규)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT | PK |
| `lesson_id` | BIGINT | FK → lecture_material |
| `student_id` | BIGINT | FK → users |
| `status` | VARCHAR(10) | PENDING / APPROVED / REJECTED |
| `requested_at` | TIMESTAMPTZ | 신청 시각 |
| `decided_at` | TIMESTAMPTZ | 결정 시각 (nullable) |
| `decided_by` | BIGINT | FK → users (PROF/ADMIN, nullable) |

제약: UNIQUE(`lesson_id`, `student_id`)

### quiz 테이블 컬럼 추가

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `lesson_id` | BIGINT | FK → lecture_material, NOT NULL |

마이그레이션:
- 기존 quiz 행이 존재한다면 작성자 PROF의 첫 번째 교안을 기본값으로 백필. 마땅한 교안이 없으면 quiz를 SOFT DELETE 처리.
- 구체 처리 방식은 구현 단계 진입 시 데이터 상태 확인 후 결정.

## 의사결정 로그

### 2026-05-22 (완료)
- **백필 마이그레이션 생략 확정**: 기존 quiz 행을 전부 폐기하고 신규로 재생성하기로 결정. 이유: 1차 출시 전 데이터로 보존 가치 낮음. 작성자 첫 교안으로 백필하는 대안은 "퀴즈-교안 실제 연관성 없음"이라는 정합성 문제로 기각.
- **`GET /api/quiz` USER 스코핑 구현**: `LessonEnrollmentRepository.findLessonIdsByStudentIdAndStatus`로 학생의 APPROVED 교안 ID 목록을 받아 `QuizRepository.findByLesson_IdIn`으로 스코핑. APPROVED 교안 없으면 `PageImpl.empty()` 단락 처리. PROF는 `findByProfessor_Id(+lessonId)`, ADMIN은 `findAll(+lessonId)` 분기.
- **WebMvcTest 슬라이스 인프라 도입**: 본 프로젝트 첫 컨트롤러 슬라이스 테스트. plan §통합 테스트 7개 시나리오를 `QuizControllerGatingTest`에서 `@MockitoBean QuizService` + `SecurityMockMvcRequestPostProcessors.user(...)`로 검증. 풀-스택 `@SpringBootTest`는 Supabase/Redis/OAuth2 의존성으로 부담이 커서 슬라이스 선택.
- **`@EnableJpaAuditing` 별도 `JpaAuditingConfig`로 분리**: 메인 애플리케이션 클래스에 붙어있던 `@EnableJpaAuditing`이 `@WebMvcTest` 컨텍스트 로딩 시 JPA metamodel 비어있음 오류를 유발. 별도 `@Configuration`으로 추출해 slice 테스트에서 `excludeFilters`로 손쉽게 제외 가능하도록 정리. 운영 동작은 동일.

### 2026-05-21 (추가)
- **`GET /api/quiz` USER 스코핑 확정**: USER 호출 시 APPROVED 교안 퀴즈만 반환하도록 변경. 이유: 미승인 교안 퀴즈가 목록에 노출되면 클릭 시 403이 뜨는 UX 불일치 발생 및 게이팅 일관성 저하.
- **`?lessonId` 쿼리 파라미터 추가**: 프론트에서 특정 교안의 퀴즈만 가져오기 위해 필수. lesson detail 응답에 quizzes 동봉하거나 클라이언트 필터링하는 대안은 응답 무거움 및 타 교수 quiz 노출 문제로 기각.
- USER + lessonId 지정 시 해당 교안이 미승인이면 403 대신 빈 페이지 반환. 이유: 목록 조회는 "존재하는 데이터 없음"에 가까운 의미이므로 403보다 빈 페이지가 적절.

### 2026-05-21
- enrollment 도메인은 `domain/lesson/` 하위에 둔다. lesson↔user join이며 lesson 도메인 책임과 가장 밀접.
- 학생은 한 교안에 한 행만 가질 수 있음. REJECTED 받은 뒤 재신청 가능 여부는 1차 범위에서 제외. (필요 시 후속 결정)
- 거절(reject)은 행을 유지하고 status만 변경. 학생의 재신청은 차단됨 (UNIQUE).
- 교수가 자신이 거절한 학생의 상태를 다시 PENDING/APPROVED로 변경 가능한지는 1차 범위에서 제외. (`PATCH /api/lessons/{id}/enrollments/{eid}/status` 후속 검토)
- **퀴즈-교안 연결 모델 — 옵션 A 채택**
  - `Quiz` 엔티티에 `lesson_id` NOT NULL FK 추가. 한 퀴즈는 한 교안에 속한다.
  - 퀴즈 생성 시 교수는 본인 소유 교안 중 하나를 지정해야 함 (소유자가 아닌 교안 지정 시 403).
  - 게이팅은 `quiz.lesson_id` 기준 enrollment APPROVED 검사.
  - 1차에서 `PUT /api/quiz/{quizId}`에서는 `lesson_id` 수정 불가 (필요 시 후속).
  - QuizQuestion.anchor_id는 그대로 유지 (오답 정리 시 페이지/문단 참조 기능).

## 테스트 케이스

### LessonEnrollmentServiceTest (예상)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `request_success` | USER 신규 신청 | PENDING 저장, `requested_at` 기록 |
| 2 | `request_duplicate` | 동일 학생-교안 재신청 | `ENROLLMENT_DUPLICATE` |
| 3 | `cancel_byStudent` | 본인 PENDING 취소 | 행 삭제 |
| 4 | `cancel_alreadyApproved` | APPROVED 취소 시도 | `ENROLLMENT_NOT_PENDING` |
| 5 | `approve_byOwner` | 교수가 본인 교안 신청 수락 | APPROVED, `decided_at`/`decided_by` 기록 |
| 6 | `approve_byOtherProf` | 다른 교수가 수락 시도 | 403 |
| 7 | `reject_byOwner` | 교수가 거절 | REJECTED, `decided_at`/`decided_by` 기록 |
| 8 | `listMine_returnsApprovedOnly` | 학생 my 조회 | APPROVED 행만 반환 |

### 통합 테스트 (퀴즈 게이팅) — `QuizControllerGatingTest`

@WebMvcTest 슬라이스. `QuizService`를 `@MockitoBean`으로 stub, `SecurityMockMvcRequestPostProcessors.user(...)`로 USER principal 주입.

| # | 메서드 | 시나리오 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `getOne_approvedUser_returns200` | APPROVED 학생이 `GET /api/quiz/{id}` | 200 |
| 2 | `getOne_unapprovedUser_returns403` | 미승인 학생이 `GET /api/quiz/{id}` | 403 `ENROLLMENT_NOT_APPROVED` |
| 3 | `submit_unapprovedUser_returns403` | 미신청 학생이 `POST /api/quiz/{id}/submit` | 403 `ENROLLMENT_NOT_APPROVED` |
| 4 | `getAll_userScopedToApproved` | USER `GET /api/quiz` (APPROVED 있음) | 해당 교안 퀴즈만 반환 |
| 5 | `getAll_userNoApproved_empty` | USER `GET /api/quiz` (APPROVED 없음) | 빈 페이지 |
| 6 | `getAll_userWithApprovedLessonId` | USER `GET /api/quiz?lessonId={approved}` | 해당 교안 퀴즈 반환 |
| 7 | `getAll_userWithUnapprovedLessonId_empty` | USER `GET /api/quiz?lessonId={unapproved}` | 빈 페이지 |

### QuizServiceTest 추가 케이스 (USER 스코핑·`lessonId` 필터)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `getAll_userScopedToApprovedLessons` | USER, `lessonId=null` | `findByLesson_IdIn(approvedIds)` 호출, 결과 반환 |
| 2 | `getAll_userNoApprovedReturnsEmpty` | USER, APPROVED 없음 | 빈 페이지, repository 미호출 |
| 3 | `getAll_userWithApprovedLessonId` | USER, `lessonId=APPROVED` | 해당 교안 퀴즈 반환 |
| 4 | `getAll_userWithUnapprovedLessonIdReturnsEmpty` | USER, `lessonId=미승인` | 빈 페이지, repository 미호출 |
| 5 | `getAll_profAllOwn` | PROF, `lessonId=null` | `findByProfessor_Id` |
| 6 | `getAll_profFilteredByLesson` | PROF, `lessonId` 지정 | `findByProfessor_IdAndLesson_Id` |
| 7 | `getAll_adminAll` | ADMIN, `lessonId=null` | `findAll` |
| 8 | `getAll_adminFilteredByLesson` | ADMIN, `lessonId` 지정 | `findByLesson_Id` |

## 작업 순서

1. 본 계획 문서 사용자 승인 + 퀴즈 연결 모델 결정
2. `docs/API.md` 신규/변경 endpoint, ErrorCode 갱신 (코드 작업 전)
3. 엔티티/리포지토리 추가
4. 서비스/컨트롤러 구현
5. 단위·통합 테스트 추가
6. `./gradlew build` 통과 확인
7. 사용자 검토 → 머지 → 본 파일 `completed/`로 이동
