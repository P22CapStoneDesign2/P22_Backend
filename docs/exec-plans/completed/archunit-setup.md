# [완료] ArchUnit 도입

- **완료일**: 2026-05-11
- **브랜치**: `refactor/arch_minsik`

## 목표

아키텍처 규칙을 문서가 아닌 코드(테스트)로 강제하여,
에이전트·팀원이 실수로 규칙을 위반해도 `./gradlew test`에서 자동 차단.

## 수용 기준

- [x] domain 서비스·리포지터리가 `global.jwt`, `global.oauth2`, `global.security`에 의존하지 않음
- [x] domain 내 Controller → Service → Repository 단방향 의존성
- [x] `dto.request/` 최상위 클래스명 → `RequestDto` 접미사
- [x] `dto.response/` 최상위 클래스명 → `ResponseDto` 접미사
- [x] `./gradlew test` 전체 통과

## 의사결정 로그

- `isOwner(Long, CustomUserDetails)` → `isOwner(Long, Long userId)` 로 서비스 시그니처 변경
  (Service 레이어가 Spring Security 객체에 의존하는 실제 위반이었음)
- `@PreAuthorize` SpEL: `principal` → `principal.userId` 로 변경
- `global.oauth2.handler`, `global.security.CustomUserDetailsService`가 domain을 참조하는 것은
  Spring Security 통합 패턴이므로 `ignoreDependency`로 허용
- 내부 클래스(nested DTO)는 `areTopLevelClasses()`로 DTO 네이밍 규칙에서 제외
