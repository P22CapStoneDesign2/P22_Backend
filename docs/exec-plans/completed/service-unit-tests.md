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
