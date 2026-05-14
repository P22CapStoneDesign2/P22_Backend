# BackEnd-dev

Spring Boot(Java 21, Gradle Kotlin DSL) 백엔드 프로젝트입니다.

## 루트 구조

- `src` — 애플리케이션 소스
- `gradle` — Gradle Wrapper 설정
- `docs` — 문서
- `reference` — 참고 자료
- `bin` — IDE 등에서 사용하는 출력 디렉터리(팀 규칙에 맞게 사용)
- `build` — Gradle 빌드 산출물(빌드 후 생성)
- `.gradle` — Gradle 캐시(로컬에서 생성)

## 빌드

```bash
./gradlew build
```

Windows에서는 `gradlew.bat build` 를 사용합니다.
