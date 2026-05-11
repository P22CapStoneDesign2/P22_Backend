# 보안 체크리스트

> 새 기능 구현 또는 PR 리뷰 시 이 목록을 확인합니다.

---

## 인증·인가

- [x] JWT Secret Key는 환경변수(`jwt.secret`)로 관리, 코드·Git에 노출 금지
- [x] Access Token 유효기간 30분
- [x] Refresh Token Rotation 적용 (재발급 시 기존 토큰 폐기)
- [x] 탈퇴(soft-delete) 유저는 `CustomUserDetails.isEnabled()` → `false`로 인증 거부
- [x] `@PreAuthorize`로 메서드 레벨 권한 검증
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
