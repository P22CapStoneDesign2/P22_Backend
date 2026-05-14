# CLAUDE.md — EQH Backend

> 이 파일은 목차(map)입니다. 세부 내용은 아래 문서를 참조하세요.
> 이 파일에 없는 결정·규칙은 존재하지 않는 것으로 간주합니다.

## 프로젝트

교수(강사)와 학생을 위한 교안 관리 및 퀴즈(문제 은행) 플랫폼.
- **패키지 루트**: `com.capstone.eqh`
- **언어**: Java 21, Spring Boot 3.x, Gradle

## 빠른 시작

```bash
./gradlew build       # 빌드 (ArchUnit 포함)
./gradlew bootRun     # 실행
./gradlew test        # 전체 테스트
```

## 핵심 불변 규칙

1. `..domain..` 패키지는 `global.jwt`, `global.oauth2`, `global.security` 의존 금지
   예외: `domain.user.service`(JWT 직접 사용하는 인증 서비스), `..domain..controller`(`@AuthenticationPrincipal` 패턴)
   → 위반 시 `./gradlew test` 빌드 실패 (`ArchitectureTest.java`)
2. DTO 클래스명: `...RequestDto` / `...ResponseDto` | 위치: `request/` / `response/`
3. 모든 API 응답은 `ApiResponse<T>` 래퍼 사용
4. 예외: `CustomException` + `ErrorCode` enum + `GlobalExceptionHandler`
5. 응답 메시지는 항상 **한국어**
6. 소유자 검증 서비스 메서드는 `isOwner(Long id, Long userId)` — `CustomUserDetails` 파라미터 금지
7. **작업 단계마다 `docs/` 의 관련 문서(exec-plan·design-doc·API·troubleshooting 등)를 최신화한 뒤, 다음 단계로 넘어가기 전 사용자 승인을 받는다** — 즉흥 작업으로 의사결정이 휘발되지 않도록 함

## 문서 지도

| 문서 | 내용 |
|------|------|
| `docs/ARCHITECTURE.md` | 패키지 구조, 레이어 의존 규칙, ArchUnit 규칙 표 |
| `docs/API.md` | 전체 API 명세 |
| `docs/PRD.md` | 서비스 기획, 권한 정의, 시나리오 |
| `docs/QUALITY.md` | 도메인별 품질 등급 및 개선 우선순위 |
| `docs/SECURITY.md` | 보안 체크리스트 |
| `docs/design-docs/index.md` | 설계 결정 목록 (왜 이렇게 만들었는가) |
| `docs/exec-plans/README.md` | **계획 작성 규칙** (새 작업 전 필독, 테스트 케이스 표 양식 포함) |
| `docs/exec-plans/tech-debt-tracker.md` | 알려진 기술 부채 목록 |
| `docs/exec-plans/active/` | 진행 중인 기능 구현 계획 |
| `docs/exec-plans/completed/` | 완료된 계획 및 의사결정 로그 |
| `docs/generated/db-schema.md` | 엔티티 기반 DB 스키마 |
| `docs/product-specs/index.md` | 제품 스펙 목록 |
| `docs/troubleshooting/index.md` | 실제 발생한 버그·환경 이슈와 해결 과정 (같은 증상 재발 시 먼저 확인) |

## 권한 및 인증

- **Role**: `PROF`(강사) · `USER`(학생) · `ADMIN`(관리자)
- **인증**: JWT — Access 30분 / Refresh 7일 (Rotation 적용)
- **소셜**: Kakao OAuth2 (OIDC)
- **Provider**: `LOCAL` · `KAKAO`
