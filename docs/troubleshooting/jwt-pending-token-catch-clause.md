# JwtProvider.getPendingTokenClaims() — 잘못된 예외 타입을 catch

- **발생일**: 2026-05-14
- **발견 경로**: 단위 테스트 작성 중 (`JwtProviderTest`)
- **관련 파일**: `src/main/java/com/capstone/eqh/global/jwt/JwtProvider.java`
- **관련 exec-plan**: `exec-plans/active/signup-role-separation.md`

## 증상

만료되었거나 다른 키로 서명된 pending 토큰을 `getPendingTokenClaims()`에 넘기면
사용자에게 의도된 `INVALID_PENDING_TOKEN` (HTTP 401, "소셜 가입 정보가 만료되었거나 유효하지 않습니다…") 대신
raw `ExpiredJwtException` 또는 `SignatureException`이 호출자까지 전파되었다.

단위 테스트에서는 다음과 같이 드러났다.

```
JwtProviderTest > getPendingTokenClaims 실패: 만료된 pending 토큰이면 INVALID_PENDING_TOKEN FAILED
JwtProviderTest > getPendingTokenClaims 실패: 다른 키로 서명된 토큰이면 INVALID_PENDING_TOKEN FAILED
```

## 원인

`getPendingTokenClaims()`의 try-catch가 `CustomException`을 잡도록 작성되어 있었다.

```java
try {
    claims = parseClaims(token);
} catch (CustomException e) {                  // ← 절대 발생하지 않음
    throw new CustomException(ErrorCode.INVALID_PENDING_TOKEN);
}
```

그러나 `parseClaims()`는 jjwt가 던지는 `JwtException` 계열(`ExpiredJwtException`,
`SignatureException`, `MalformedJwtException` …)이나 `IllegalArgumentException`을
그대로 던진다 — **`CustomException`은 발생할 수 없다.** 따라서 catch 블록이 한 번도 실행되지
않았고, jjwt 예외가 그대로 호출자(컨트롤러 → `GlobalExceptionHandler`)까지 빠져나갔다.

운영에서는 카카오 신규 유저가 가입 페이지에서 10분 이상 머문 경우 친절한 에러 대신
의도하지 않은 에러 응답이 나갔을 가능성이 있다.

(참고: 같은 클래스의 `validateToken()`은 처음부터 `JwtException | IllegalArgumentException`을
잡도록 올바르게 작성되어 있었다. `getPendingTokenClaims()`만 패턴을 따라가지 못한 단발 실수.)

## 조치

catch 절을 jjwt가 실제로 던지는 예외 타입으로 교체.

```diff
 try {
     claims = parseClaims(token);
-} catch (CustomException e) {
+} catch (JwtException | IllegalArgumentException e) {
     throw new CustomException(ErrorCode.INVALID_PENDING_TOKEN);
 }
```

`JwtException`은 jjwt의 모든 토큰 검증 예외(`ExpiredJwtException` 포함)의 상위 타입이므로
하나로 만료·위변조·형식 오류를 모두 흡수한다.

## 검증

`JwtProviderTest`에 5개 케이스 추가, 그중 만료·위변조 케이스가 수정 후 통과한다.

| 케이스 | 입력 | 기대 |
|--------|------|------|
| `getPendingTokenClaims_typeMismatch` | access 토큰을 pending으로 파싱 | `INVALID_PENDING_TOKEN` |
| `getPendingTokenClaims_tampered` | 다른 키로 서명된 토큰 | `INVALID_PENDING_TOKEN` |
| `getPendingTokenClaims_expired` | 과거 시각으로 만료된 토큰 | `INVALID_PENDING_TOKEN` |

```bash
./gradlew test --tests "com.capstone.eqh.global.jwt.JwtProviderTest"
```

## 교훈 / 재발 방지

- **catch 절은 라이브러리가 실제로 던지는 예외 타입을 잡아야 한다.** `CustomException`은
  도메인 경계 안에서만 발생하므로, 외부 라이브러리(jjwt, JDBC, Jackson 등) 호출을 감싸는
  catch에 `CustomException`이 등장하면 거의 확실히 잘못된 코드다.
- 토큰 파싱처럼 외부 입력을 다루는 메서드는 **위변조·만료 케이스의 단위 테스트가 필수**다.
  정상 흐름만 테스트하면 이 부류의 버그를 잡을 수 없다.
