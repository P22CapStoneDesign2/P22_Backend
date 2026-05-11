# CLAUDE.md — EQH Backend

## 프로젝트 개요
교수(강사)와 학생을 위한 교안 관리 및 퀴즈(문제 은행) 플랫폼입니다.
- **패키지 루트**: `com.capstone.eqh`
- **언어 및 프레임워크**: Java 21, Spring Boot 3.x, Gradle

## 빌드 및 실행 명령어
- **빌드**: `./gradlew build`
- **애플리케이션 실행**: `./gradlew bootRun`
- **클린 빌드**: `./gradlew clean build`

## 테스트 명령어
- **전체 테스트 실행**: `./gradlew test`
- **특정 테스트 클래스 실행**: `./gradlew test --tests "패키지명.클래스명"`
- **테스트 가이드**: JUnit5 및 Mockito를 사용하며, 서비스 레이어 단위 테스트는 필수입니다.

## 아키텍처 및 패키지 구조
- **계층 구조**: Controller → Service → Repository
- **도메인 분리**: `domain/{user,quiz,lesson}/`
- **공통/전역 설정**: `global/{jwt,oauth2,security,exception,common}/`
- **규칙**: domain 패키지는 global 패키지의 인증·권한 로직에 의존하지 않아야 합니다.

## 코딩 컨벤션
### 1. DTO (Data Transfer Object)
- **네이밍**: 반드시 `...RequestDto.java` 또는 `...ResponseDto.java` 형식을 사용합니다. (예: `SignupRequestDto`, `AuthResponseDto`)
- **구조**: DTO는 반드시 `request/` 또는 `response/` 하위 패키지로 분리합니다.

### 2. 엔티티 (Entity)
- **Lombok**: `@Getter`, `@Builder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)` 사용을 권장합니다.

### 3. 응답 및 예외 처리
- **공통 응답**: 모든 API 응답은 `ApiResponse<T>` 래퍼 클래스를 사용합니다.
- **예외 처리**: `CustomException`, `ErrorCode` enum, `GlobalExceptionHandler`를 통해 처리합니다.
- **언어**: 사용자 응답 메시지는 항상 **한국어**를 사용합니다.

### 4. 컨트롤러 및 서비스
- **컨트롤러**: URL 경로 기준으로 분리합니다.
    - `/api/auth/**` -> `AuthController`
    - `/api/users/**` -> `UserController`
- **서비스**: 의존성 그래프를 기준으로 명확히 분리합니다.

## 권한 및 인증 (RBAC)
- **역할(Role)**: `PROF` (강사), `USER` (학생), `ADMIN` (관리자)
- **인증 방식**: JWT (Access 30분 / Refresh 7일) 및 Kakao OAuth2
- **제공자(AuthProvider)**: `LOCAL`, `KAKAO`

## 참고 문서
- **기획**: `docs/PRD.md`
- **API 명세**: `docs/API.md`
- **상세 구조**: `docs/ARCHITECTURE.md`
