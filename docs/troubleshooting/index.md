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
| [jwt-filter-public-401.md](jwt-filter-public-401.md) | `JwtFilter`가 `permitAll` 엔드포인트에서도 토큰 검증을 시도해, 헤더의 stale 토큰 때문에 회원가입·재발급이 401로 차단되던 문제 | 2026-05-14 |
| [supabase-pooler-session-max-clients.md](supabase-pooler-session-max-clients.md) | Supabase Supavisor session mode(pool_size=15)를 좀비 JDBC 세션이 점유해 `bootRun` 시 `EMAXCONNSESSION`으로 `entityManagerFactory` 빈 생성이 실패하던 문제 | 2026-05-19 |
| [lecture-material-content-not-null.md](lecture-material-content-not-null.md) | `lecture_material.content` 컬럼에 NOT NULL 제약이 잔존해 교안 생성(POST /api/lessons) 시 500 발생. `ddl-auto: update`는 NOT NULL 제거를 하지 않음 | 2026-05-24 |
| [lecture-material-missing-lesson-id.md](lecture-material-missing-lesson-id.md) | 강의/교안 분리 후 `lecture_material.lesson_id` 컬럼이 실제 테이블에 추가되지 않아 교안 생성 500. `ddl-auto: update`는 기존 행이 있는 테이블에 NOT NULL 컬럼을 추가하지 못함. `Quiz.material` FK 도 `lesson_material_id` 로 리네임 | 2026-05-25 |
