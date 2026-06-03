# ARCHITECTURE — EQH Backend 패키지 구조

> **패키지 루트**: `com.capstone.eqh`

---

## 전체 구조

```
src/main/java/com/capstone/eqh/
│
├── eqhApplication.java
│
├── domain/
│   ├── user/
│   │   ├── entity/
│   │   │   ├── User.java                              # status (PENDING/ACTIVE/REJECTED) 포함
│   │   │   └── RefreshToken.java
│   │   ├── enums/
│   │   │   ├── Role.java                              # PROF / USER / ADMIN
│   │   │   ├── AuthProvider.java                      # LOCAL / KAKAO
│   │   │   └── UserStatus.java                        # PENDING / ACTIVE / REJECTED — PROF 승인 상태
│   │   ├── repository/
│   │   │   ├── UserRepository.java                    # findByRoleAndStatus 포함
│   │   │   └── RefreshTokenRepository.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── LoginRequestDto.java
│   │   │   │   ├── ProfSignupRequestDto.java
│   │   │   │   ├── UserSocialSignupRequestDto.java
│   │   │   │   ├── UserUpdateRequestDto.java
│   │   │   │   ├── UserDeleteRequestDto.java
│   │   │   │   ├── ProfessorStatusUpdateRequestDto.java # ADMIN PATCH status 본문
│   │   │   │   ├── ReissueRequestDto.java
│   │   │   │   └── LogoutRequestDto.java
│   │   │   └── response/
│   │   │       ├── AuthResponseDto.java               # accessToken + (PROF 가입 시) status
│   │   │       ├── UserResponseDto.java               # status 포함
│   │   │       ├── PendingProfessorResponseDto.java   # ADMIN pending 목록 항목
│   │   │       └── ProfessorStatusResponseDto.java    # ADMIN approve/reject/status 응답
│   │   ├── service/
│   │   │   ├── UserAuthService.java                   # 로그인, 토큰 재발급, 로그아웃, profSignup(토큰 발급 위임)
│   │   │   ├── UserSignupService.java                 # 회원가입 — PROF는 status=PENDING, REJECTED 이메일 재가입 차단
│   │   │   ├── UserService.java                       # 프로필 조회, 수정, 탈퇴
│   │   │   └── AdminProfessorService.java             # ADMIN: pending 목록, approve, reject, 임의 상태 변경
│   │   └── controller/
│   │       ├── AuthController.java                    # /api/auth/**
│   │       ├── UserController.java                    # /api/users/**
│   │       └── AdminProfessorController.java          # /api/admin/professors/**
│   │
│   ├── quiz/
│   │   ├── entity/
│   │   │   ├── Quiz.java                              # quiz 테이블 — 퀴즈 세트
│   │   │   ├── QuizQuestion.java                      # quiz_q 테이블 — 문제 (교안 참조 포함)
│   │   │   ├── QuizOption.java                        # quiz_opt 테이블 — 객관식 보기
│   │   │   ├── QuizSubmission.java                    # quiz_sub 테이블 — 학생 제출
│   │   │   └── QuizSubmissionAnswer.java              # quiz_sub_answer 테이블 — 문제별 답안
│   │   ├── enums/
│   │   │   └── QuizType.java                          # MULTIPLE_CHOICE / SHORT_ANSWER
│   │   ├── repository/
│   │   │   ├── QuizRepository.java
│   │   │   ├── QuizQuestionRepository.java
│   │   │   ├── QuizSubmissionRepository.java
│   │   │   └── QuizSubmissionAnswerRepository.java    # findWrongAnswersByStudentId 포함
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── QuizCreateRequestDto.java
│   │   │   │   ├── QuizUpdateRequestDto.java
│   │   │   │   ├── QuizQuestionCreateRequestDto.java  # anchorId, lessonPage, lessonParagraph 포함
│   │   │   │   ├── QuizQuestionUpdateRequestDto.java
│   │   │   │   └── QuizSubmitRequestDto.java
│   │   │   └── response/
│   │   │       ├── QuizResponseDto.java               # 퀴즈 세트 요약
│   │   │       ├── QuizDetailResponseDto.java         # 퀴즈 세트 + 문제 목록
│   │   │       ├── QuizQuestionResponseDto.java       # 문제 + 교안 참조 (anchorId, lessonPage, lessonParagraph)
│   │   │       ├── QuizSubmissionResponseDto.java     # 제출 결과 + 채점
│   │   │       └── WrongAnswerResponseDto.java        # 오답 + lessonRef (교안 참조 상세)
│   │   ├── service/
│   │   │   └── QuizService.java
│   │   └── controller/
│   │       └── QuizController.java                    # /api/quiz/**
│   │
│   └── lesson/
│       ├── entity/
│       │   ├── Lesson.java                            # lesson 테이블 — 강의 (교안의 상위)
│       │   ├── LessonMaterial.java                    # lecture_material 테이블 — 교안 (lesson_id FK NOT NULL)
│       │   └── LessonEnrollment.java                  # lesson_enrollment 테이블 — 학생 수강 신청
│       ├── enums/
│       │   └── EnrollmentStatus.java                  # PENDING / APPROVED / REJECTED
│       ├── repository/
│       │   ├── LessonRepository.java
│       │   ├── LessonMaterialRepository.java
│       │   └── LessonEnrollmentRepository.java
│       ├── dto/
│       │   ├── request/
│       │   │   ├── LessonCreateRequestDto.java        # 강의 및 교안 생성 본문 재사용
│       │   │   └── LessonUpdateRequestDto.java        # 강의 및 교안 수정 본문 재사용
│       │   └── response/
│       │       ├── LessonResponseDto.java
│       │       ├── LessonMaterialResponseDto.java     # 교안 단건/목록 응답
│       │       ├── EnrollmentResponseDto.java         # 수강 신청 단건
│       │       ├── EnrollmentDecisionResponseDto.java # 수락/거절 결과
│       │       ├── EnrollmentListItemResponseDto.java # 교수용 신청 목록 항목 (학생 정보 포함)
│       │       └── MyLessonResponseDto.java           # 학생 my 강의 목록 항목 (approvedAt 포함)
│       ├── service/
│       │   ├── LessonService.java
│       │   ├── LessonMaterialService.java
│       │   └── LessonEnrollmentService.java
│       └── controller/
│           ├── LessonController.java                  # /api/lessons/**
│           ├── LessonMaterialController.java          # /api/lessons/{id}/materials/**
│           ├── LessonEnrollmentController.java        # /api/lessons/{id}/enrollments/**, /api/lessons/my
│           └── LessonAdminController.java             # /api/admin/lessons/**
│
└── global/
    ├── jwt/
    │   ├── JwtProvider.java                           # 토큰 생성, 검증, 파싱
    │   └── JwtFilter.java                             # 모든 요청 JWT 검사
    │
    ├── oauth2/
    │   ├── CustomOidcUser.java                        # OidcUser 래퍼 (dbUserId, dbUserRole 포함)
    │   ├── info/
    │   │   ├── OAuth2UserInfo.java                    # 추상 클래스
    │   │   └── KakaoOAuth2UserInfo.java               # OIDC claims (sub, nickname) 파싱
    │   ├── service/
    │   │   └── CustomOidcUserService.java             # OIDC OAuth2 어댑터 — 유저 저장/조회는 UserSignupService에 위임
    │   └── handler/
    │       ├── OAuth2SuccessHandler.java              # JWT 발급 및 리다이렉트
    │       └── OAuth2FailureHandler.java              # 로그인 실패 처리
    │
    ├── security/
    │   ├── SecurityConfig.java                        # FilterChain 및 RBAC
    │   ├── PasswordConfig.java                        # PasswordEncoder 빈 (순환 의존성 분리)
    │   ├── CustomUserDetails.java
    │   └── CustomUserDetailsService.java
    │
    ├── exception/
    │   ├── ErrorCode.java
    │   ├── CustomException.java
    │   └── GlobalExceptionHandler.java
    │
    └── common/
        ├── ApiResponse.java                           # 공통 응답 규격
        └── BaseTimeEntity.java                        # createdAt / updatedAt 자동 관리
```

---

## 도메인 책임 요약

| 도메인 | 경로 | 주요 역할 |
|--------|------|-----------|
| `user` | `/api/auth/**`, `/api/users/**`, `/api/admin/professors/**` | 인증, 회원가입(PROF 승인 워크플로 포함), 프로필 관리, ADMIN의 PROF 승인/거절 |
| `quiz` | `/api/quiz/**` | 퀴즈 세트·문제 관리, 채점, 오답 조회 (퀴즈는 단일 교안에 속함) |
| `lesson` | `/api/lessons/**` | 강의·교안 관리, 학생 수강 신청·교수 수락 (강의 1 ──< 교안 N) |

## global 책임 요약

| 패키지 | 주요 역할 |
|--------|-----------|
| `jwt` | 토큰 생성·검증·파싱, 요청 필터링 |
| `oauth2` | 카카오 OIDC OAuth2 어댑터 — 유저 생성 비즈니스 로직은 `UserSignupService`에 위임 |
| `security` | FilterChain 구성, RBAC 적용, PasswordEncoder 빈 |
| `exception` | 공통 예외 처리 |
| `common` | 공통 응답 규격, JPA Auditing 기반 시간 필드 |

---

## 퀴즈-교안 연동 설계

### 엔티티 관계

```
Lesson (1) ──── (N) LessonMaterial             ← lesson_id FK (NOT NULL) — 강의-교안 소속
Quiz (N) ──── (1) LessonMaterial               ← lesson_material_id FK (NOT NULL) — 게이팅 기준 (material.lesson 으로 한 다리)
Quiz (1) ──── (N) QuizQuestion
QuizQuestion (N) ──── (1) LessonMaterial       ← anchor_id FK (nullable) — 페이지/문단 참조
QuizQuestion (1) ──── (N) QuizOption

LessonEnrollment (N) ──── (1) Lesson           ← lesson_id FK (NOT NULL)
LessonEnrollment (N) ──── (1) User (student)
LessonEnrollment (N) ──── (0..1) User (decidedBy)   ← PROF/ADMIN

QuizSubmission (N) ──── (1) Quiz
QuizSubmission (N) ──── (1) User (student)
QuizSubmissionAnswer (N) ──── (1) QuizSubmission
QuizSubmissionAnswer (N) ──── (1) QuizQuestion
```

### 교안 참조 필드 (QuizQuestion / quiz_q)

| 필드 | DB 컬럼 | 설명 |
|------|---------|------|
| `anchor` | `anchor_id` | 참조 교안 FK → `lecture_material` (nullable) |
| `lessonPage` | `lesson_page` | 교수가 지정한 교안 페이지 번호 |
| `lessonParagraph` | `lesson_paragraph` | 교수가 지정한 교안 문단 번호 |

### PROF 승인 게이팅 데이터 흐름

```
PROF 가입: POST /api/auth/profsignup
  → UserSignupService.profSignup
  → 이메일 인증 검증 → 기존 이메일이 REJECTED 면 EMAIL_REJECTED 409
  → User 저장 (status=PENDING)
  → UserAuthService가 토큰 발급 → AuthResponseDto(status=PENDING) 반환

PROF 전용 호출 (예: POST /api/lessons):
  → @PreAuthorize("hasRole('PROF') and principal.active")
  → CustomUserDetails.isActive() = (user.status == ACTIVE)
  → PENDING/REJECTED 면 AccessDeniedException
  → GlobalExceptionHandler가 principal 검사 → PROF + 비활성 → PROF_NOT_APPROVED 403

ADMIN 승인: POST /api/admin/professors/{id}/approve
  → AdminProfessorService.approveProfessor → User.updateStatus(ACTIVE)
```

### 학생 수강 게이팅 데이터 흐름

```
USER → GET /api/quiz/{quizId}                            (또는 POST submit)
  → QuizService.assertEnrolledIfStudent(quiz, userId)
  → LessonEnrollmentRepository.existsByLessonIdAndStudentIdAndStatus(quiz.material.lesson.id, userId, APPROVED)
  → false 이면 ENROLLMENT_NOT_APPROVED 403
```

USER 가 `GET /api/lessons/my` 호출 시:
```
LessonEnrollmentRepository.findApprovedByStudentId(userId, pageable)
  → MyLessonResponseDto (lesson + approvedAt)
```

### 오답 조회 데이터 흐름

```
GET /api/quiz/wrong-answers
 → QuizSubmissionAnswerRepository.findWrongAnswersByStudentId()
 → QuizSubmissionAnswer → QuizQuestion → LessonMaterial (anchor)
 → WrongAnswerResponseDto { lessonRef { lessonId, lessonTitle, lessonPage, lessonParagraph } }
 (lessonRef.lessonId 는 응답 필드명이며 실제 ID 는 lecture_material.id — 호환 유지를 위해 필드명 미변경)
```

---

## 설계 원칙

- **Role 정의** (`enums/Role.java`)는 `domain/user/`에 위치 — 비즈니스 데이터
- **Role 강제** (`SecurityConfig.java`)는 `global/security/`에 위치 — 횡단 관심사
- 퀴즈·교안 도메인의 소유자 검증은 컨트롤러 `@PreAuthorize` + `isOwner(Long id, Long userId)`로 처리
  - 서비스의 `isOwner`는 `Long userId`만 받음 — `CustomUserDetails` 등 보안 객체를 받지 않음
  - 컨트롤러에서 `principal.userId`를 꺼내 서비스에 전달 (`@PreAuthorize("... isOwner(#id, principal.userId)")`)
- `domain.*.service`, `domain.*.repository`는 `global.jwt`, `global.oauth2`, `global.security`에 의존하지 않음
  - 예외: `domain.user.service` — `UserAuthService`가 `JwtProvider`를 직접 사용하는 인증 서비스
- DTO 네이밍: `...RequestDto` / `...ResponseDto`, 구조: `request/` / `response/` 분리
- Service는 의존성 그래프 기준으로 분리
  - `UserAuthService` — 로그인, 토큰 재발급, 로그아웃, PROF 가입 후 토큰 발급(`profSignup`)
  - `UserSignupService` — 회원가입 (LOCAL은 PROF + status=PENDING으로 저장), 소셜 유저 조회/생성 (`findOrCreateSocialUser`)
  - `UserService` — 프로필 조회, 수정, 탈퇴
  - `AdminProfessorService` — ADMIN의 PROF 승인 워크플로 (pending 목록, approve, reject, 임의 status 변경)
  - `QuizService` — 퀴즈 CRUD, 문제 관리, 채점, 오답 조회. USER 호출 시 `LessonEnrollmentRepository`로 게이팅
  - `LessonEnrollmentService` — 학생 신청, 교수 수락/거절, my 조회
- Controller는 URL 경로 기준으로 분리
  - `AuthController` — `/api/auth/**`
  - `UserController` — `/api/users/**`
  - `AdminProfessorController` — `/api/admin/professors/**` (ADMIN-only, SecurityConfig `requestMatchers("/api/admin/**").hasRole("ADMIN")` 으로 보호)
  - `QuizController` — `/api/quiz/**`
  - `LessonController` — `/api/lessons/**`
  - `LessonEnrollmentController` — `/api/lessons/{id}/enrollments/**`, `/api/lessons/my`
- **PROF 승인 게이팅**: PROF 전용 엔드포인트 `@PreAuthorize`는 `principal.active` 조건을 포함하여 PENDING/REJECTED PROF의 호출을 차단. `CustomUserDetails.isActive()`가 `user.status == ACTIVE` 반환. 차단 시 `GlobalExceptionHandler`가 `AccessDeniedException`을 `PROF_NOT_APPROVED`(403)로 변환
- **소셜 로그인 유저 식별**: `provider` + `providerId` (카카오 OIDC `sub` claim) 조합
- `PasswordEncoder` 빈은 순환 의존성 방지를 위해 `PasswordConfig`에 별도 분리
- **퀴즈 채점**: `quiz_q.correct_answer`와 `student_answer`를 대소문자 무시 비교
- **퀴즈 재제출 방지**: `quiz_sub` UNIQUE(quiz_id, student_id) + 서비스 레이어 중복 검사

---

## 아키텍처 강제 적용 (ArchUnit)

`src/test/java/com/capstone/eqh/ArchitectureTest.java`에서 빌드 시 자동으로 검증하는 규칙:

| 규칙 | 내용 |
|------|------|
| `domainServiceDoesNotDependOnGlobalAuth` | domain 서비스·리포지터리는 `global.jwt`, `global.oauth2`, `global.security` 의존 불가 |
| `domainLayeringRule` | domain 내부 Controller → Service → Repository 단방향 의존성 강제 |
| `requestDtoNamingRule` | `dto.request` 패키지의 최상위 클래스는 `RequestDto`로 끝나야 함 |
| `responseDtoNamingRule` | `dto.response` 패키지의 최상위 클래스는 `ResponseDto`로 끝나야 함 |

규칙 위반 시 `./gradlew test` 빌드가 실패하므로, 잘못된 의존성이 코드베이스에 병합되지 않습니다.
