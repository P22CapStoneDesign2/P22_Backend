# CLAUDE.md — EQH Backend

> 이 파일은 EQH 백엔드 작업을 위한 프로젝트 헌장과 문서 지도입니다.  
> 세부 구현 규칙은 `docs/` 문서를 따릅니다.  작업 단계마다 docs/ 의 관련 문서를 최신화한 뒤, 다음 단계로 넘어가기 전 사용자 승인을 받는다
> 이 파일과 이 파일이 명시적으로 참조하는 `docs/` 문서에 없는 결정·규칙은 임의로 새로 만들지 않습니다.

## 프로젝트

교수(강사)와 학생을 위한 교안 관리 및 퀴즈(문제 은행) 플랫폼 — 백엔드.

- **패키지 루트**: `com.capstone.eqh`
- **언어/프레임워크**: Java 21, Spring Boot 3.x
- **빌드 도구**: Gradle
- **주요 사용자 역할**: `PROF`(강사), `USER`(학생), `ADMIN`(관리자)

## 빠른 시작

```bash
./gradlew build       # 전체 빌드, 테스트, ArchUnit 검사 포함
./gradlew bootRun     # 애플리케이션 실행
./gradlew test        # 전체 테스트 실행
```

## 핵심 원칙

1. 백엔드는 API 계약의 원본입니다.
   - API endpoint, 요청 DTO, 응답 DTO, 에러 응답은 `docs/API.md`를 기준으로 합니다.
   - API 계약을 변경하면 `docs/API.md`를 반드시 갱신합니다.
   - 프론트엔드와 함께 작업하는 경우 프론트 `docs/API-SPEC.md`도 같은 작업 단계에서 갱신합니다.

2. 도메인 계층은 보안 구현 세부사항에 의존하지 않습니다.
   - `..domain..` 패키지는 `global.jwt`, `global.oauth2`, `global.security`에 의존하지 않습니다.
   - 도메인 서비스는 Spring Security 객체나 JWT 구현체를 직접 알지 않도록 합니다.
   - 인증된 사용자 정보가 필요한 경우 controller에서 userId, role 등 필요한 값만 추출해 service로 전달합니다.

3. 계층 경계를 우회하지 않습니다.
   - Controller는 요청 검증, 인증 주체 추출, 응답 변환에 집중합니다.
   - Service는 비즈니스 로직과 트랜잭션 경계를 담당합니다.
   - Repository는 영속성 접근을 담당합니다.
   - Entity를 API 응답으로 직접 노출하지 않고 ResponseDto로 변환합니다.

4. 모든 일반 JSON REST API 응답은 `ApiResponse<T>` 래퍼를 사용합니다.
   - 성공 응답과 실패 응답 모두 일관된 형태를 유지합니다.
   - OAuth redirect, 파일 다운로드, 정적 리소스, actuator/health, SSE/streaming 등은 예외가 될 수 있습니다.
   - 예외 응답은 `docs/API.md`에 명시합니다.

5. 예외 처리는 표준 구조를 사용합니다.
   - 비즈니스 예외는 `CustomException`과 `ErrorCode`를 사용합니다.
   - 전역 예외 처리는 `GlobalExceptionHandler`에서 담당합니다.
   - controller/service에서 임의의 `RuntimeException`, 문자열 에러, 임시 Map 응답을 만들지 않습니다.

6. 응답 메시지는 항상 한국어로 작성합니다.
   - 성공 메시지
   - 실패 메시지
   - 검증 오류 메시지
   - 인증/인가 오류 메시지
   - 사용자에게 전달될 가능성이 있는 모든 메시지를 포함합니다.

7. 인증과 인가는 중앙 보안 계층에서 관리합니다.
   - JWT 발급, 검증, 재발급, OAuth2 처리는 `global.jwt`, `global.oauth2`, `global.security` 등 보안 계층에서 담당합니다.
   - 일반 도메인 로직에 JWT 파싱, 토큰 생성, SecurityContext 직접 조작 로직을 넣지 않습니다.

8. 소유자 검증은 보안 객체가 아니라 식별자 기반으로 수행합니다.
   - 소유자 검증 메서드는 `isOwner(Long id, Long userId)` 형태를 사용합니다.
   - `CustomUserDetails`를 service 소유자 검증 메서드의 파라미터로 넘기지 않습니다.

9. 보안 관련 흐름은 임의로 변경하지 않습니다.
   - 로그인, 로그아웃, 토큰 재발급, Refresh Token Rotation, Kakao OAuth2 흐름은 `docs/SECURITY.md`와 `docs/ARCHITECTURE.md`를 따릅니다.
   - 인증/인가 흐름 변경은 사용자 승인 없이 진행하지 않습니다.

10. 작업 단계마다 관련 문서를 갱신합니다.
    - API, 인증, 인가, 도메인 정책, 예외 코드, 환경 변수, 보안 정책이 바뀌면 관련 `docs/` 문서를 함께 수정합니다.
    - 다음 단계로 넘어가기 전 사용자 승인을 받습니다.

## 아키텍처 경계

### 패키지 의존 규칙

`..domain..` 패키지는 다음 패키지에 의존하지 않습니다.

```txt
global.jwt
global.oauth2
global.security
```

허용된 예외:

- `domain.user.service`
  - 로그인, 토큰 발급, 인증 서비스 등 인증 도메인과 직접 맞닿은 서비스
- `..domain..controller`
  - `@AuthenticationPrincipal`을 통해 인증 주체를 받는 controller 패턴

예외를 새로 추가해야 하는 경우:

1. 먼저 `docs/ARCHITECTURE.md`에 이유를 기록합니다.
2. 필요한 경우 `ArchitectureTest.java`의 ArchUnit 규칙을 갱신합니다.
3. 사용자 승인을 받은 뒤 구현합니다.

### ArchUnit

아키텍처 의존 규칙은 테스트로 강제합니다.

- 관련 테스트: `ArchitectureTest.java`
- `./gradlew test` 또는 `./gradlew build` 실패 시 아키텍처 위반으로 간주합니다.
- 테스트를 우회하거나 삭제하지 않습니다.
- 규칙 변경이 필요하면 먼저 문서와 실행 계획을 갱신합니다.

## API 설계 원칙

### API 계약

백엔드 API 계약의 원본은 다음 문서입니다.

```txt
docs/API.md
```

API 변경 시 반드시 함께 갱신할 항목:

- endpoint path
- HTTP method
- request body
- query parameter
- path variable
- response body
- error code
- 권한 조건
- 인증 필요 여부
- 예외 응답

프론트엔드와 연동되는 API를 변경하는 경우 프론트 문서도 함께 갱신합니다.

```txt
frontend/docs/API-SPEC.md
```

### 응답 형식

일반 JSON REST API는 `ApiResponse<T>`를 사용합니다.

원칙:

- 성공 응답도 `ApiResponse<T>` 사용
- 실패 응답도 `ApiResponse<T>` 또는 동일한 에러 envelope 사용
- controller에서 raw DTO, raw String, raw Map을 직접 반환하지 않습니다.
- 예외 응답도 프론트에서 일관되게 파싱할 수 있어야 합니다.

예외 가능 항목:

- OAuth redirect
- 파일 다운로드
- 이미지/PDF/binary 응답
- 정적 리소스
- actuator/health
- SSE/streaming

예외 API는 반드시 `docs/API.md`에 명시합니다.

### DTO 규칙

DTO 클래스명은 다음 규칙을 따릅니다.

```txt
...RequestDto
...ResponseDto
```

DTO 위치는 다음 규칙을 따릅니다.

```txt
request/
response/
```

원칙:

- RequestDto는 외부 요청 입력을 표현합니다.
- ResponseDto는 외부 응답 출력을 표현합니다.
- Entity를 API 요청/응답으로 직접 사용하지 않습니다.
- controller 외부로 노출되는 객체는 API 계약과 일치해야 합니다.

## 예외 처리 원칙

### 표준 예외 구조

비즈니스 예외는 다음 구조를 사용합니다.

```txt
CustomException
ErrorCode
GlobalExceptionHandler
```

원칙:

- 비즈니스 실패는 `CustomException`으로 표현합니다.
- 에러 종류는 `ErrorCode` enum에 정의합니다.
- 예외 응답 변환은 `GlobalExceptionHandler`에서 담당합니다.
- 사용자에게 노출되는 메시지는 한국어로 작성합니다.
- 임시 문자열 예외, 임시 HTTP 응답, controller 단위 중복 예외 처리를 만들지 않습니다.

### 인증/인가 예외

Spring Security 필터 단계에서 발생하는 인증/인가 실패도 일관된 응답 형태를 유지해야 합니다.

원칙:

- 401 인증 실패는 표준 한국어 에러 응답을 반환합니다.
- 403 인가 실패는 표준 한국어 에러 응답을 반환합니다.
- 가능한 경우 `AuthenticationEntryPoint`, `AccessDeniedHandler`에서 `ApiResponse` 형식과 맞춥니다.
- 프론트엔드가 401과 403을 명확히 구분할 수 있어야 합니다.

## 인증 / 인가

### 인증 방식

- 인증 방식: JWT
- Access Token 유효기간: 30분
- Refresh Token 유효기간: 7일
- Refresh Token Rotation 적용
- 소셜 로그인: Kakao OAuth2 / OIDC

### 역할

| Role | 의미 |
|------|------|
| `PROF` | 강사 |
| `USER` | 학생 |
| `ADMIN` | 관리자 |

### 인증/인가 원칙

- 최종 권한 검증은 백엔드가 수행합니다.
- 프론트엔드의 권한 분기는 UX 보조 수단일 뿐입니다.
- 권한이 필요한 API는 controller 또는 security 설정에서 명확히 보호합니다.
- service 계층은 필요한 경우 userId, role 같은 값만 받아 도메인 정책을 판단합니다.
- service 계층에 JWT 문자열, SecurityContext, CustomUserDetails를 무분별하게 전달하지 않습니다.

### 토큰 재발급

Refresh Token Rotation을 사용합니다.

원칙:

- 재발급 성공 시 새로운 Access Token과 Refresh Token 쌍을 발급합니다.
- 기존 Refresh Token의 재사용 가능 여부와 만료 정책은 `docs/SECURITY.md`에 명시합니다.
- 재발급 API의 요청/응답 형식은 `docs/API.md`와 일치해야 합니다.
- 토큰 값은 로그에 남기지 않습니다.

### Kakao OAuth2

Kakao OAuth2/OIDC 흐름은 보안 문서와 API 문서를 기준으로 합니다.

관련 문서:

- `docs/SECURITY.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`

OAuth redirect URI, callback path, 프론트 redirect URL 등 환경별로 달라지는 값은 설정으로 관리합니다.  
소스 코드에 운영/개발 호스트를 직접 하드코딩하지 않습니다.

## 환경 설정 원칙

환경별로 달라지는 값은 코드에 하드코딩하지 않습니다.

대상 예시:

- DB 접속 정보
- JWT secret
- Kakao OAuth client id / secret
- redirect URI
- CORS allowed origins
- 프론트엔드 공개 URL
- 외부 API URL
- 파일 저장 경로

원칙:

- 민감 정보는 Git에 커밋하지 않습니다.
- 운영 secret을 테스트 코드나 문서 예시에 넣지 않습니다.
- 환경 변수 또는 Spring profile 설정을 사용합니다.
- CORS 허용 origin은 환경별 설정으로 관리합니다.
- 새 환경 변수를 추가하면 관련 문서와 예시 설정을 함께 갱신합니다.

## 보안 원칙

다음 항목은 금지합니다.

- 토큰, 비밀번호, 인증 코드, OAuth code를 로그에 출력
- 민감 정보를 예외 메시지에 포함
- 운영 secret을 저장소에 커밋
- 인증/인가 검증을 프론트엔드에 의존
- controller에서 임시로 권한 검증 생략
- 테스트 통과를 위해 보안 설정을 무력화
- CORS를 근거 없이 전체 허용으로 변경
- 도메인 서비스에서 JWT 직접 파싱

보안 정책 변경이 필요하면 먼저 `docs/SECURITY.md`와 실행 계획을 갱신하고 사용자 승인을 받습니다.

## 문서 운영 규칙

새 작업을 시작하기 전 다음을 확인합니다.

1. `docs/exec-plans/README.md`
2. 관련 `docs/ARCHITECTURE.md`
3. 관련 `docs/API.md`
4. 관련 `docs/SECURITY.md`
5. 기존 active/completed exec-plan
6. `docs/troubleshooting/`의 관련 이슈

다음 변경이 있으면 문서 갱신이 필수입니다.

- API endpoint 추가/변경
- request/response DTO 변경
- ErrorCode 추가/변경
- 인증/인가 흐름 변경
- 역할/권한 정책 변경
- 환경 변수 추가/변경
- 도메인 정책 변경
- DB 스키마 또는 Entity 구조 변경
- 보안 정책 변경
- 알려진 버그 또는 해결 과정 발생

작업 흐름:

1. 작업 범위를 확인합니다.
2. 필요한 경우 `docs/exec-plans/active/`에 실행 계획을 작성합니다.
3. 관련 설계 문서를 최신화합니다.
4. 사용자 승인을 받은 뒤 구현합니다.
5. 구현 후 문서와 코드가 일치하는지 확인합니다.
6. 완료된 계획은 `docs/exec-plans/completed/`로 이동하고 의사결정 로그를 남깁니다.

## 품질 기준

작업 완료 전 가능한 경우 아래 명령을 실행합니다.

```bash
./gradlew test
./gradlew build
```

`./gradlew build`는 ArchUnit을 포함한 전체 품질 게이트로 간주합니다.

다음은 허용하지 않습니다.

- 문서와 다른 임의 구현
- API 계약을 갱신하지 않은 endpoint 변경
- Entity 직접 응답
- `ApiResponse<T>` 우회
- `CustomException` / `ErrorCode` 우회
- 사용자 노출 메시지의 영문 방치
- 도메인 계층의 보안 구현 의존
- controller/service에 중복 인증 처리 구현
- 소유자 검증에 `CustomUserDetails` 직접 전달
- 테스트 실패 상태로 완료 보고
- 보안 설정 임시 완화 후 방치

새 의존성 추가, 아키텍처 변경, 인증 흐름 변경, API 계약 변경, DB 구조 변경은 사용자 승인 없이 진행하지 않습니다.

## 문서 지도

| 문서 | 내용 |
|------|------|
| `docs/ARCHITECTURE.md` | 패키지 구조, 레이어 의존 규칙, ArchUnit 규칙 |
| `docs/API.md` | 백엔드 REST API 명세 |
| `docs/PRD.md` | 서비스 기획, 권한 정의, 사용자 시나리오 |
| `docs/QUALITY.md` | 도메인별 품질 등급 및 개선 우선순위 |
| `docs/SECURITY.md` | 백엔드 보안 체크리스트 |
| `docs/design-docs/index.md` | 설계 결정 목록 |
| `docs/exec-plans/README.md` | 실행 계획 작성 규칙, 테스트 케이스 표 양식 |
| `docs/exec-plans/tech-debt-tracker.md` | 알려진 기술 부채 목록 |
| `docs/exec-plans/active/` | 진행 중인 기능 구현 계획 |
| `docs/exec-plans/completed/` | 완료된 계획 및 의사결정 로그 |
| `docs/generated/db-schema.md` | Entity 기반 DB 스키마 |
| `docs/product-specs/index.md` | 제품 스펙 목록 |
| `docs/troubleshooting/index.md` | 실제 발생한 버그·환경 이슈와 해결 과정 |

## 작업 전 확인 질문

구현 중 아래 중 하나라도 해당하면 즉시 멈추고 문서 또는 사용자 확인을 우선합니다.

- 필요한 API가 `docs/API.md`에 없는가?
- 응답 DTO 구조가 불명확한가?
- 어떤 ErrorCode를 써야 하는지 모호한가?
- 권한 기준이 모호한가?
- 소유자 검증 기준이 불명확한가?
- 새 환경 변수가 필요한가?
- Entity 또는 DB 스키마 변경이 필요한가?
- 인증/OAuth/JWT 흐름을 바꿔야 하는가?
- 기존 아키텍처 경계를 우회해야만 구현 가능한가?
- ArchUnit 규칙 변경이 필요한가?
- 프론트엔드 API 계약에도 영향이 있는가?