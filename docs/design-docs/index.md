# Design Docs — 설계 결정 목록

> 에이전트·개발자가 코드 변경 시 관련 설계 결정을 먼저 확인하세요.
> 이 파일에 없는 결정은 존재하지 않는 것으로 간주합니다 (Slack·구두 합의 금지).

| 문서 | 상태 | 대상 도메인 | 요약 |
|------|------|------------|------|
| [auth-flow.md](auth-flow.md) | ✅ 확정 | user, global | JWT + Kakao OAuth2 인증 흐름, Refresh Token Rotation |
| [rbac-model.md](rbac-model.md) | ✅ 확정 | 전체 | PROF/USER/ADMIN 권한 체계, @PreAuthorize 패턴 |
| [signup-role-separation.md](signup-role-separation.md) | ✅ 확정 | user, global.oauth2, global.jwt | PROF 로컬 가입 / USER 카카오 가입 분리, pending 토큰 흐름 |
