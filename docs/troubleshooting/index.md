# Troubleshooting — 이슈 해결 기록

> 실제 발생한 버그·환경 이슈와 그 해결 과정을 보존합니다.
> "왜 이렇게 고쳤는가"를 미래의 개발자가 git blame 없이 알 수 있도록 하는 것이 목적입니다.
> 같은 증상이 다시 나타나면 먼저 여기를 확인하세요.

---

## 작성 양식

각 이슈 파일은 다음 섹션을 포함합니다.

```markdown
# {{한 줄 제목}}

- **발생일**: YYYY-MM-DD
- **발견 경로**: 단위 테스트 / 통합 테스트 / 운영 / 코드 리뷰 등
- **관련 파일**: `path/to/File.java`
- **관련 exec-plan / design-doc**: (있다면)

## 증상
어떤 동작·에러 메시지가 나왔는가.

## 원인
왜 발생했는가. 잘못된 코드/설정의 구체적 지점.

## 조치
무엇을 바꿨는가. (diff 요점 또는 핵심 라인)

## 검증
어떻게 확인했는가. (테스트 케이스, 명령어)

## 교훈 / 재발 방지
같은 부류의 버그를 다시 만들지 않으려면. (선택)
```

---

## 이슈 목록

| 파일 | 요약 | 발생일 |
|------|------|--------|
| [jwt-pending-token-catch-clause.md](jwt-pending-token-catch-clause.md) | `JwtProvider.getPendingTokenClaims()`의 catch가 잘못된 예외 타입을 잡아 만료·위변조 시 `INVALID_PENDING_TOKEN`으로 변환되지 않던 문제 | 2026-05-14 |
| [jwt-secret-key-length.md](jwt-secret-key-length.md) | `application-local.yaml`의 JWT secret이 248비트라 jjwt 0.12.x의 HMAC-SHA 최소 요구(256비트)에 미달해 Spring Context 로딩 실패 | 2026-05-14 |
