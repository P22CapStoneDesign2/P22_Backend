# [완료] 서비스 레이어 단위 테스트 작성

- **완료일**: 2026-05-11
- **브랜치**: `test/minsik`
- **해결한 부채**: TD-001

## 목표

`CLAUDE.md`에 명시된 "서비스 레이어 단위 테스트 필수" 규칙을 충족시킨다.
JUnit5 + Mockito 기반으로 `UserAuthService`, `LessonService`, `QuizService`의 핵심 동작과 예외 흐름을 검증한다.

## 수용 기준

- [x] `UserAuthService` 단위 테스트: login (5케이스), reissue (4케이스), logout (2케이스)
- [x] `LessonService` 단위 테스트: create, getOne, update, delete, isOwner (10케이스)
- [x] `QuizService` 단위 테스트: create, update, isOwner, submit (9케이스)
- [x] 모든 `CustomException` 발생 경로 검증
- [x] `isOwner(Long, Long userId)` 시그니처 동작 검증 (TD-007 회귀 방지)
- [x] `./gradlew test` 전체 통과 (35개 테스트)

## 의사결정 로그

- 엔티티 ID는 `ReflectionTestUtils.setField`로 주입
  (JPA `@GeneratedValue` 필드라 setter 없음, Mock보다 실제 객체가 DTO 변환 검증에 유리)
- 채점 로직(`submit`)은 대소문자 무시(`equalsIgnoreCase`) 동작을 별도 케이스로 검증
- 알 수 없는 `questionId` 무시 동작도 별도 케이스로 검증
- Mockito `STRICT_STUBS` 기본 모드 사용 — 불필요한 stub 제거로 테스트 신뢰도 확보

## 테스트 커버리지

| 서비스 | 테스트 수 | 위치 |
|--------|----------|------|
| `UserAuthService` | 11 | `src/test/java/com/capstone/eqh/domain/user/service/UserAuthServiceTest.java` |
| `LessonService` | 10 | `src/test/java/com/capstone/eqh/domain/lesson/service/LessonServiceTest.java` |
| `QuizService` | 9 | `src/test/java/com/capstone/eqh/domain/quiz/service/QuizServiceTest.java` |

---

## 테스트 케이스 상세

### UserAuthServiceTest (11)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `login_success` | 유효한 자격증명 | 토큰 페어 발급 + RefreshToken 저장 |
| 2 | `login_userNotFound` | 존재하지 않는 이메일 | `USER_NOT_FOUND` |
| 3 | `login_softDeletedUser` | 탈퇴(soft-delete) 유저 | `USER_NOT_FOUND` |
| 4 | `login_socialAccount` | KAKAO 계정으로 일반 로그인 | `SOCIAL_ACCOUNT_CONFLICT` |
| 5 | `login_wrongPassword` | 비밀번호 불일치 | `INVALID_CREDENTIALS` |
| 6 | `reissue_invalidToken` | 토큰에서 userId 추출 불가 | `INVALID_TOKEN` |
| 7 | `reissue_tokenNotInDb` | DB에 저장된 토큰 없음 | `INVALID_TOKEN` |
| 8 | `reissue_expiredToken` | 만료된 토큰 | DB 삭제 + `EXPIRED_TOKEN` |
| 9 | `reissue_success` | 정상 재발급 | 새 토큰 + 기존 토큰 갱신 |
| 10 | `logout_deletesToken` | 토큰 존재 | DB에서 삭제 |
| 11 | `logout_noOpIfNotFound` | 토큰 없음 | delete 호출 안 함 |

### LessonServiceTest (10)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `create_success` | 정상 생성 | DTO 반환 + save 호출 |
| 2 | `create_userNotFound` | 존재하지 않는 사용자 | `USER_NOT_FOUND` |
| 3 | `getOne_success` | 단건 조회 | 교안 정보 반환 |
| 4 | `update_success` | 제목·설명 갱신 | DTO에 변경 사항 반영 |
| 5 | `update_notFound` | 존재하지 않는 교안 | `LESSON_NOT_FOUND` |
| 6 | `delete_notFound` | 존재하지 않는 교안 | `LESSON_NOT_FOUND` |
| 7 | `isOwner_true` | 생성자 ID 일치 | `true` |
| 8 | `isOwner_notOwner` | 생성자 ID 불일치 | `false` |
| 9 | `isOwner_lessonNotFound` | 교안 없음 | `false` (예외 아님) |
| 10 | `isOwner_creatorNull` | `createdBy`가 null | `false` |

### QuizServiceTest (9)

| # | 메서드 | 검증 내용 | 기대 결과 |
|---|--------|----------|----------|
| 1 | `create_success` | 정상 생성 | DTO 반환 (professorId 일치) |
| 2 | `create_userNotFound` | 존재하지 않는 사용자 | `USER_NOT_FOUND` |
| 3 | `update_notFound` | 존재하지 않는 퀴즈 | `QUIZ_NOT_FOUND` |
| 4 | `isOwner_true` | 출제 교수 ID 일치 | `true` |
| 5 | `isOwner_notOwner` | 출제 교수 ID 불일치 | `false` |
| 6 | `isOwner_quizNotFound` | 퀴즈 없음 | `false` |
| 7 | `submit_alreadySubmitted` | 재제출 시도 | `QUIZ_ALREADY_SUBMITTED` |
| 8 | `submit_success` | 대소문자 무시 채점 | "fcfs" == "FCFS" 정답 처리, 총점 계산 |
| 9 | `submit_ignoresUnknownQuestion` | 알 수 없는 questionId | 무시하고 정상 문제만 채점 |
