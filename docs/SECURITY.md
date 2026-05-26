# 보안 체크리스트

> 새 기능 구현 또는 PR 리뷰 시 이 목록을 확인합니다.

---

## 인증·인가

- [x] JWT Secret Key는 환경변수(`jwt.secret`)로 관리, 코드·Git에 노출 금지
- [x] JWT Secret Key는 디코드된 바이트 길이 ≥ 32 (HS256 최소 요구 256비트) — 참고: [troubleshooting/jwt-secret-key-length.md](troubleshooting/jwt-secret-key-length.md)
- [x] Access Token 유효기간 30분
- [x] Refresh Token Rotation 적용 (재발급 시 기존 토큰 폐기)
- [x] 카카오 신규 유저 가입 임시 토큰(`PENDING_SOCIAL`)은 10분 유효, DB 저장 없이 JWT 클레임만으로 상태 유지 — 만료·위변조 시 `INVALID_PENDING_TOKEN`(401) 반환
- [x] `JwtFilter`는 `/api/auth/**`, `/oauth2/**`, `/login/oauth2/**` 경로에서 `shouldNotFilter()`로 토큰 검증을 skip — 회원가입·로그인·재발급이 헤더의 stale 토큰에 의해 차단되는 것을 방지 — 참고: [troubleshooting/jwt-filter-public-401.md](troubleshooting/jwt-filter-public-401.md)
- [x] 탈퇴(soft-delete) 유저는 `CustomUserDetails.isEnabled()` → `false`로 인증 거부
- [x] `@PreAuthorize`로 메서드 레벨 권한 검증
- [x] PROF 가입 직후 `status=PENDING` 저장 — ADMIN 승인(`ACTIVE`) 전까지 PROF 전용 기능(교안/퀴즈 CRUD, 수강 신청 수락) 사용 불가
  - `CustomUserDetails.isActive()` 기반으로 `@PreAuthorize("hasRole('PROF') and principal.active ...")` 평가
  - 비활성 PROF가 차단되면 `GlobalExceptionHandler`가 `AccessDeniedException`을 `PROF_NOT_APPROVED`(403, "교수 계정 승인 대기 중입니다.")로 변환
  - 토큰 자체는 가입 시 정상 발급되므로 `GET /api/users/me`로 상태 확인 가능
- [x] 가입 거절된(`REJECTED`) 이메일로 PROF 재가입 시도는 `EMAIL_REJECTED`(409)로 차단. DB 행은 유지되며 ADMIN이 `PATCH /api/admin/professors/{id}/status`로 재활성화 가능
- [ ] Refresh Token 탈취 감지 (동일 토큰 재사용 감지) 미구현

## 입력 검증

- [x] `@Valid` + Jakarta Validation으로 DTO 입력값 검증
- [x] `GlobalExceptionHandler`에서 `MethodArgumentNotValidException` 처리
- [ ] SQL Injection: JPA 파라미터 바인딩 사용 (Spring Data JPA 기본 보호)
- [ ] 서비스 레이어 직접 호출 시 소유자 검증 없음 (TD-003)

## 비밀번호

- [x] BCryptPasswordEncoder로 해시 저장
- [x] 소셜 로그인 계정은 `password` NULL (비밀번호 변경 불가 처리)
- [x] `PasswordConfig`를 분리해 순환 의존성 방지

## 데이터 노출

- [x] `correctAnswer`는 퀴즈 상세 조회 응답에서 학생에게 미노출
- [x] `UserResponseDto`에 password 필드 없음

## 환경별 설정

- [ ] `application-local.yaml`에 실제 DB 비밀번호 하드코딩 여부 확인
- [ ] 운영 환경 시크릿 관리 방안 미수립 (환경변수 또는 Secret Manager 도입 필요)
