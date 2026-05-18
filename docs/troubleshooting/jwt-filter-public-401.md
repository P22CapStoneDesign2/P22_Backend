# JwtFilter — public 엔드포인트가 stale 토큰 때문에 401로 막히는 문제

- **발생일**: 2026-05-14
- **발견 경로**: 운영(로컬 환경) — 카카오 로그인 전 회원가입 시도
- **관련 파일**: `src/main/java/com/capstone/eqh/global/jwt/JwtFilter.java`
- **관련 exec-plan**: `exec-plans/active/jwt-filter-public-skip.md`

## 증상

브라우저에 만료되었거나 옛 secret으로 서명된 access token이 남아 있는 상태에서
`POST /api/auth/profsignup`을 호출하면 컨트롤러까지 도달하지 못하고
다음 응답이 반환되었다.

```json
{ "status": 401, "message": "유효하지 않은 토큰입니다.", "data": null }
```

재현 시나리오:
1. 기존 access token이 있는 상태에서 백엔드의 `jwt.secret` 값을 교체
2. (또는) access token이 만료된 채로 30분 이상 경과
3. 회원가입 폼 제출 → axios가 Authorization 헤더에 옛/만료 토큰을 자동 첨부
4. JwtFilter가 컨트롤러 진입 전에 401로 차단

## 원인

`SecurityConfig`에서 `/api/auth/**`는 `permitAll()`로 열려 있지만,
`JwtFilter.doFilterInternal()`은 **모든 요청**에서 Authorization 헤더의 토큰을
검증하도록 작성되어 있었다.

```java
if (token == null) {
    filterChain.doFilter(request, response);
    return;
}
try {
    jwtProvider.validateToken(token);  // ← invalid/expired면 CustomException
    ...
} catch (CustomException e) {
    sendErrorResponse(response, e.getErrorCode());  // ← 401 즉시 응답, chain 중단
    return;
}
```

즉 인가(Authorization) 측면에서는 익명 허용이지만, 필터 체인 안의 토큰 검증 단계에서
"헤더에 토큰이 실려 있으면 무조건 유효해야 한다"는 암묵적 가정이 깔려 있었다.
회원가입·로그인·재발급은 정의상 토큰 없이도(또는 만료된 채로) 호출되어야 하는데
이 가정과 충돌한다.

특히 다음 두 엔드포인트는 동작 자체가 access token과 무관함:
- `/api/auth/reissue` — refresh token(request body)으로 새 access token을 발급
- `/api/auth/logout` — refresh token(request body)을 폐기

이들조차 헤더 access token이 유효해야만 호출 가능했던 셈이라 reissue가 영원히 불가능한
경계 상황이 발생할 수 있었다.

## 조치

`OncePerRequestFilter`의 `shouldNotFilter()`를 오버라이드하여 public 경로 prefix에서는
필터 자체를 skip하도록 변경.

```java
private static final String[] PUBLIC_PATH_PREFIXES = {
        "/api/auth/",
        "/oauth2/",
        "/login/oauth2/"
};

@Override
protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getServletPath();
    for (String prefix : PUBLIC_PATH_PREFIXES) {
        if (path.startsWith(prefix)) {
            return true;
        }
    }
    return false;
}
```

이러면 해당 경로에서는 `doFilterInternal()`이 호출되지 않아 토큰 검증도, 인증 컨텍스트
설정도 일어나지 않는다. 컨트롤러는 익명 상태로 호출된다.

## 검증

`JwtFilterTest`에 4개 케이스 추가, 모두 통과.

| 케이스 | 입력 | 기대 |
|--------|------|------|
| `skipsValidation_onAuthPath` | `/api/auth/profsignup` + invalid token | `jwtProvider.validateToken` 미호출, chain 통과 |
| `skipsValidation_onOAuth2Path` | `/oauth2/authorization/kakao` + expired token | `jwtProvider.validateToken` 미호출 |
| `skipsValidation_onLoginOAuth2Path` | `/login/oauth2/code/kakao` + stale token | `jwtProvider.validateToken` 미호출 |
| `validatesToken_onProtectedPath` | `/api/users/me` + token | `jwtProvider.validateToken` 1회 호출 |

```bash
./gradlew test --tests "com.capstone.eqh.global.jwt.JwtFilterTest"
```

## 교훈 / 재발 방지

- **`permitAll()`은 "토큰 없어도 OK"이지 "토큰이 무효해도 OK"가 아니다.** Spring Security의
  허용 정책과 별도로, 커스텀 필터 안에서 토큰 검증을 시도하면 결과적으로 더 엄격한
  제한이 걸리게 된다. 익명 API를 만들 때는 필터 레벨에서도 명시적으로 화이트리스트화해야 한다.
- **`reissue` / `logout`처럼 토큰 라이프사이클을 다루는 엔드포인트일수록** 해당 경로에서
  access token 검증을 강제하면 안 된다 (자기-잠금 위험).
- **client-side stale token은 일반적인 현상이다.** 백엔드 secret 교체, 만료, 다중 탭 등으로
  쉽게 발생한다. 익명 호출 가능성이 있는 경로는 처음부터 토큰 검증을 우회하도록 설계한다.
