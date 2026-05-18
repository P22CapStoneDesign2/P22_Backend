# JWT secret 키 길이 부족 — WeakKeyException으로 Spring Context 로딩 실패

- **발생일**: 2026-05-14
- **발견 경로**: `./gradlew test` 실행 시 `EqzBackendApplicationTests.contextLoads()` 실패
- **관련 파일**: `src/main/resources/application-local.yaml`
- **관련 exec-plan**: `exec-plans/active/signup-role-separation.md` (블로커로 해결)

## 증상

`./gradlew test` 실행 시 컨텍스트 로딩 단계에서 다음 예외 발생.

```
EqzBackendApplicationTests > contextLoads() FAILED
  Caused by: io.jsonwebtoken.security.WeakKeyException:
    The specified key byte array is 248 bits which is not secure enough for any
    JWT HMAC-SHA algorithm. … MUST have a size >= 256 bits …
    at io.jsonwebtoken.security.Keys.hmacShaKeyFor(Keys.java:83)
    at com.capstone.eqh.global.jwt.JwtProvider.<init>(JwtProvider.java:36)
```

`JwtProvider` 빈 생성이 실패하면서 의존 빈 전체가 함께 로드 실패 → 모든 테스트가 사실상 마비된다.

## 원인

`application-local.yaml`의 `jwt.secret` 값이 base64 디코드 시 **31바이트(248비트)** 였다.

```yaml
jwt:
  secret: yZ/gR9gwt7p2T/oJsu7WnjAo7PpuDXHPY53Fa+7lv0=   # 31 bytes
```

jjwt 0.12.x의 `Keys.hmacShaKeyFor()`는 RFC 7518 §3.2에 따라 HMAC-SHA 키가 해시 출력
크기 이상이어야 함을 강제한다. HS256은 최소 256비트(32바이트)가 필요하므로 31바이트
키는 거부된다.

이 키는 회원가입 분리 작업과 무관하게 이전부터 존재했으나, 동일 환경에서 같은
테스트가 통과했는지 여부는 불명확하다. (jjwt 버전 업이나 키 값 변경 시점이 가려져
있을 수 있음.)

## 조치

`openssl rand -base64 32`로 32바이트 키를 생성해 교체.

```diff
 jwt:
-  secret: yZ/gR9gwt7p2T/oJsu7WnjAo7PpuDXHPY53Fa+7lv0=
+  secret: eznHPpYI/9FkNLt1ngJQTxaFfjePbMjqghP2e2fVrUE=
```

> 이 키는 로컬 개발 전용이다. 배포 환경 키는 별도로 관리하며 절대 커밋하지 않는다.

## 검증

```bash
./gradlew test
# BUILD SUCCESSFUL — 50 tests passed
```

## 교훈 / 재발 방지

- JWT secret을 새로 발급할 때는 `openssl rand -base64 32` (HS256) / `48` (HS384) /
  `64` (HS512) 와 같이 알고리즘에 맞는 길이로 생성한다. base64 인코딩 후 길이가 아닌
  **디코드된 바이트 길이**가 기준임에 주의.
- 환경별 `application-*.yaml`의 `jwt.secret` 길이 검증은 현재 자동화되어 있지 않다.
  필요하면 `@PostConstruct`에서 `secretKey.getEncoded().length` 를 확인해
  명시적 메시지로 실패하게 만드는 보강을 고려할 수 있다 (현재는 jjwt가 던지는
  `WeakKeyException` 메시지가 충분히 친절하므로 즉시 도입하지는 않음).
