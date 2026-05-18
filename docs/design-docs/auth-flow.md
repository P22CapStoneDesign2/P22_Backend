# 인증 흐름 설계 결정

> 상태: ✅ 확정 | 대상: `domain/user/`, `global/jwt/`, `global/oauth2/`

---

## 결정 1 — JWT Stateless 방식 채택

**결정**: 세션 없이 JWT (Access + Refresh) 기반 인증.

**이유**: REST API 서버로 프론트엔드(React)와 분리 운영, 수평 확장 고려.

**트레이드오프**: 토큰 강제 무효화가 어려움
→ Refresh Token을 DB(`refresh_tokens`)에 저장하여 로그아웃/재발급 시 폐기 처리.

| 토큰 | 유효기간 | 저장 위치 | 포함 정보 |
|------|---------|-----------|----------|
| Access Token | 30분 | 클라이언트 | userId, role |
| Refresh Token | 7일 | 클라이언트 + DB | userId |

---

## 결정 2 — Refresh Token Rotation 적용

**결정**: 재발급 시 기존 Refresh Token 폐기, 새 토큰 발급.

**이유**: Refresh Token 탈취 시 피해 최소화.

**구현**: `UserAuthService.reissue()` — DB에서 기존 토큰 삭제 후 새 토큰 저장.

---

## 결정 3 — UserAuthService의 JwtProvider 의존 허용

**결정**: `domain.user.service.UserAuthService`는 `global.jwt.JwtProvider`를 직접 사용.

**이유**: UserAuthService의 핵심 책임이 인증 토큰 발급이므로 불가피한 의존.

**경계**: 다른 domain 서비스(LessonService, QuizService 등)는 global.jwt 의존 금지.
→ `ArchitectureTest.domainServiceDoesNotDependOnGlobalAuth` 규칙으로 자동 강제.

---

## 결정 4 — Service 레이어에 CustomUserDetails 전달 금지

**결정**: Controller에서 `principal.userId`(Long)만 추출해 Service에 전달.

**이유**: Service는 Spring Security 객체를 알 필요가 없음 → 단위 테스트 용이, 레이어 결합도 감소.

**구현**:
```java
// Controller @PreAuthorize
@PreAuthorize("hasRole('PROF') and @lessonService.isOwner(#id, principal.userId)")

// Service 시그니처
public boolean isOwner(Long lessonId, Long userId) { ... }
```

---

## 결정 5 — Soft Delete 적용

**결정**: `users` 테이블에 `deleted`(BOOLEAN), `deleted_at`(TIMESTAMP)으로 Soft Delete.

**이유**: 퀴즈 제출 이력 등 연관 데이터 참조 무결성 유지.

**구현**: `CustomUserDetails.isEnabled()` → `!user.isDeleted()` — 탈퇴 유저 인증 자동 거부.

---

## 결정 6 — Kakao 소셜 로그인 식별자

**결정**: 유저 식별을 `provider` + `providerId` 조합으로 처리 (이메일 아님).

**이유**: 동일 이메일로 LOCAL 계정이 존재해도 KAKAO 계정은 별도 처리 가능.

**구현**: `CustomOidcUserService` — OIDC `sub` claim을 `providerId`로 저장.

---

## 결정 7 — CustomOidcUserService 책임 분리

**결정**: 소셜 유저 조회/생성 비즈니스 로직을 `CustomOidcUserService`에서 `UserSignupService`로 이전.

**이유**: `CustomOidcUserService`는 Spring Security `OidcUserService`를 상속하는 프레임워크 어댑터다. 유저 생성 로직이 함께 있으면 `global/oauth2`가 도메인 비즈니스 규칙(닉네임 생성 정책 등)을 직접 보유하게 되어 관심사가 혼재된다.

**구현**:
```
CustomOidcUserService.loadUser()
  → userSignupService.findOrCreateSocialUser(providerId, name, provider)
      → UserRepository.findByProviderAndProviderId()  // 기존 유저 조회
      → createSocialUser() + generateUniqueNickname() // 신규 유저 생성
```

**경계**: `CustomOidcUserService`는 OAuth2 프로토콜 처리 + `UserSignupService` 위임만 담당. 닉네임 생성 정책은 `UserSignupService` 내부.
