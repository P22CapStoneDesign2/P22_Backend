# lesson(강의) / lecture_material(교안) 분리

- **시작일**: 2026-05-24
- **브랜치**: dev
- **관련 결정**: 강의 1:N 교안, 수강 신청 강의 단위, 퀴즈는 교안 소속 유지

## 목표

현재 `lecture_material` 테이블 하나가 강의·교안 역할을 겸하고 있어 수강 신청 단위가 불명확하다.  
강의(`lesson`)를 신설하고 교안(`lecture_material`)을 그 하위에 두어 계층 구조를 명확히 한다.

```
lesson (강의, NEW)
  ├── lesson_enrollment (수강 신청 — 강의 단위)
  └── lecture_material (교안, 현 Lesson 개명)
        └── quiz → quiz_q
```

## 확정 설계 결정

| 항목 | 결정 |
|------|------|
| 수강 신청 단위 | 강의 (lesson) |
| 퀴즈 소속 | 교안 (lecture_material) 유지 |
| 강의:교안 비율 | 1:N |
| 기존 데이터 | 전체 삭제 후 재시작 |

## API 변경 요약

### 강의 (신규)
| Method | Path | 권한 |
|--------|------|------|
| `POST` | `/api/lessons` | PROF |
| `GET` | `/api/lessons` | 인증 필요 |
| `GET` | `/api/lessons/{lessonId}` | 인증 필요 |
| `PUT` | `/api/lessons/{lessonId}` | PROF(본인)/ADMIN |
| `DELETE` | `/api/lessons/{lessonId}` | PROF(본인)/ADMIN |

### 교안 (경로 변경)
| Method | 이전 | 이후 |
|--------|------|------|
| `POST` | `/api/lessons` | `/api/lessons/{lessonId}/materials` |
| `GET` | `/api/lessons` | `/api/lessons/{lessonId}/materials` |
| `GET` | `/api/lessons/{id}` | `/api/lessons/{lessonId}/materials/{materialId}` |
| `PUT` | `/api/lessons/{id}` | `/api/lessons/{lessonId}/materials/{materialId}` |
| `DELETE` | `/api/lessons/{id}` | `/api/lessons/{lessonId}/materials/{materialId}` |

### 수강 신청 (경로 유지, 참조 대상 변경)
경로는 동일(`/api/lessons/{lessonId}/enrollments`).  
`lessonId`가 `lecture_material.id` → `lesson.id` (강의)로 변경됨.

### 퀴즈 (DTO 필드명 변경)
`QuizCreateRequestDto.lessonId` → `materialId`로 rename.  
`GET /api/quiz?lessonId=X` 파라미터도 `materialId`로 변경.

---

## 수용 기준

- [x] DB: `lesson` 테이블 생성, `lecture_material.lesson_id` FK 추가, `lesson_enrollment.lesson_id` → `lesson.id` 참조
- [x] 엔티티: `Lesson` → `LessonMaterial` (교안), 신규 `Lesson` (강의)
- [x] 강의 CRUD: `POST/GET/PUT/DELETE /api/lessons`
- [x] 교안 CRUD: `POST/GET/PUT/DELETE /api/lessons/{lessonId}/materials`
- [x] 교안 생성 시 부모 강의 소유자 검증
- [x] 교안 목록/조회: 강의에 APPROVED 수강 중인 USER만 접근
- [x] 수강 신청: `lesson_id` → 강의 ID 기준
- [x] `GET /api/lessons/my`: 내 승인 강의 목록 반환
- [x] 퀴즈: `materialId` 파라미터로 생성, 교안에 속한 강의의 수강 여부로 게이팅
- [x] `./gradlew test` 통과

---

## 구현 단계

### Step 0: DB 초기화 SQL (사용자 직접 실행)

```sql
DROP TABLE IF EXISTS quiz_sub_answer CASCADE;
DROP TABLE IF EXISTS quiz_sub CASCADE;
DROP TABLE IF EXISTS quiz_opt CASCADE;
DROP TABLE IF EXISTS quiz_q CASCADE;
DROP TABLE IF EXISTS quiz CASCADE;
DROP TABLE IF EXISTS lesson_enrollment CASCADE;
DROP TABLE IF EXISTS lecture_material CASCADE;
-- lesson 테이블은 아직 없으므로 생략
-- users, refresh_tokens 는 유지
```

이후 `bootRun` 시 JPA `ddl-auto: update`가 새 스키마로 테이블 자동 생성.

### Step 1: 엔티티 변경
- `Lesson.java` → `LessonMaterial.java` (`lesson_id` FK 필드 추가)
- `Lesson.java` 신규 생성 (강의, 테이블 `lesson`)
- `LessonEnrollment.java`: `lesson` 필드 타입 `Lesson(교안)` → `Lesson(강의)`

### Step 2: Repository 변경
- `LessonRepository` → `LessonMaterialRepository`
- 신규 `LessonRepository` (강의용)
- `LessonEnrollmentRepository`: JPQL 경로 점검

### Step 3: Service 변경
- `LessonService` → `LessonMaterialService` (교안 CRUD, 소유자 검증 포함)
- 신규 `LessonService` (강의 CRUD)
- `LessonEnrollmentService`: `Lesson(강의)` 참조로 전환

### Step 4: Controller / DTO 변경
- 신규 `LessonController` (`/api/lessons` — 강의)
- `LessonController` → `LessonMaterialController` (`/api/lessons/{lessonId}/materials`)
- `LessonAdminController`: 강의 목록 반환으로 변경
- `LessonEnrollmentController`: `lessonService` → 강의 서비스 참조 전환
- DTO rename: `LessonResponseDto` → `LessonResponseDto`(강의), `LessonMaterialResponseDto`(교안)
- `MyLessonResponseDto`: enrollment → 강의 정보 반환

### Step 5: Quiz 관련 변경
- `Quiz.java`: `lesson` 필드 → `material` (타입 `LessonMaterial`)
- `QuizCreateRequestDto`: `lessonId` → `materialId`
- `QuizService`: `create()`, `getAll()`, `findQuizzesForStudent()`, `assertEnrolledIfStudent()`, `isOwner()` 등 수정
- `QuizRepository`: derived query 명칭 변경
- `QuizQuestion.java`: `anchor` 타입 `Lesson` → `LessonMaterial`

### Step 6: 테스트 수정
- `LessonServiceTest`, `LessonEnrollmentServiceTest` fixture 수정
- `LessonControllerProfGatingTest`, `LessonEnrollmentControllerGatingTest` 수정
- `QuizServiceTest`, `QuizControllerGatingTest`, `QuizControllerProfGatingTest` 수정

### Step 7: 문서 갱신
- `docs/API.md` 전면 갱신 (강의/교안 분리)
- `docs/generated/db-schema.md` 갱신

---

## ErrorCode 추가/변경

| 코드 | 변경 |
|------|------|
| `LESSON_NOT_FOUND` | 강의를 찾을 수 없습니다. (기존 유지) |
| `LESSON_MATERIAL_NOT_FOUND` | 교안을 찾을 수 없습니다. (신규) |
| `LESSON_MATERIAL_NOT_IN_LESSON` | 해당 강의에 속한 교안이 아닙니다. (신규) |

## 의사결정 로그

- **2026-05-24** — 기존 데이터 전체 삭제 후 재시작 (마이그레이션 SQL 불필요)
- **2026-05-24** — 수강 신청 단위: 강의. 강의 수강 승인 시 해당 강의의 모든 교안 접근 가능
- **2026-05-24** — 퀴즈 소속: 교안 유지. quiz.lesson_id 컬럼명 유지, 참조 대상만 lecture_material 그대로
- **2026-05-24** — 교안 CRUD 경로: `/api/lessons/{lessonId}/materials` (강의 ID 명시로 소유자 검증 단순화)
- **2026-05-24** — `QuizCreateRequestDto.lessonId` → `materialId` rename (의미 명확화)
- **2026-05-25** — 구현 완료. `./gradlew build` 통과. 모든 테스트 통과. docs/API.md, docs/generated/db-schema.md 갱신 완료
