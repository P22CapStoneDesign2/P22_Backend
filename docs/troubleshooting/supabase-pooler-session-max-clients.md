# Supabase 풀러 세션 모드 한도 초과 — bootRun 시 EMAXCONNSESSION

- **발생일**: 2026-05-19
- **발견 경로**: 로컬 개발 중 `./gradlew bootRun` 실행 시 컨텍스트 로딩 실패
- **관련 파일**: `src/main/resources/application-local.yaml`
- **관련 exec-plan / design-doc**: 없음 (운영 외부 리소스 이슈)

## 증상

`./gradlew bootRun --args='--spring.profiles.active=local'` 실행 시
Hibernate가 DDL용 JDBC 연결을 열다가 다음 예외로 부팅이 중단된다.

```
FATAL: (EMAXCONNSESSION) max clients reached in session mode
  - max clients are limited to pool_size: 15

Caused by: org.postgresql.util.PSQLException
  → org.hibernate.exception.GenericJDBCException:
      Unable to open JDBC Connection for DDL execution
  → BeanCreationException: 'entityManagerFactory'
  → ApplicationContextException: Unable to start web server
```

연쇄적으로 `userRepository`, `customUserDetailsService`, `jwtFilter` 빈 생성이
모두 실패해 Tomcat 자체가 뜨지 않는다.

---

## 잘못된 이유

**Supabase Supavisor 풀러의 세션 모드 슬롯이 좀비 JDBC 세션으로 가득 차서**
새로운 클라이언트 연결을 받지 못한 것이다.

구조적 원인은 다음 세 가지가 겹쳤다.

1. **연결 URL이 풀러의 세션 모드(5432)를 향한다.**
   `application-local.yaml`의 datasource URL이
   `aws-1-ap-northeast-2.pooler.supabase.com:5432`로,
   Supavisor의 **session mode** 엔드포인트다.
   무료/저가 플랜에서 이 모드의 동시 클라이언트 한도는 **pool_size: 15**.
   같은 풀러 호스트라도 포트 `6543`은 transaction mode이며 한도가 훨씬 크다.

2. **HikariCP의 기본 풀 사이즈(maximum-pool-size = 10)와 한도가 거의 같다.**
   `application-local.yaml`에 HikariCP 풀 크기 설정이 없으므로 기본값 10이 적용된다.
   bootRun 한 번이 최대 10개 슬롯을 잡고, 두 번째 인스턴스가 뜨려고 하면
   풀러 한도(15)를 넘어선다.

3. **이전 bootRun들이 정상 종료되지 않아 idle 세션이 풀러 측에 남았다.**
   Supabase Roles 페이지 기준 `postgres` 롤에 **15개의 idle JDBC 세션**이 누적
   (`application_name = 'PostgreSQL JDBC Driver'`, 모두 `state = 'idle'`).
   누적된 좀비가 한도 15를 그대로 점유하니, 새 부팅 시도는 즉시 거부됐다.

> 주의: 비밀번호 평문 노출, CLAUDE.md "환경 설정 원칙" 위반 등 **같은 yaml 파일에
> 별도로 존재하는 보안 이슈는 본 문서의 범위가 아니다.** 별도 정리 필요.

---

## 시도한 방법

해결 전 단계로 다음 진단·조작을 거쳤다.

### 1. 로컬 좀비 프로세스 확인 — 음성

```bash
ps -ef | grep -E "java|gradle" | grep -v grep
```

VS Code의 Java/Gradle 데몬만 떠 있고 bootRun 프로세스는 없음.
즉 **로컬에는 연결을 잡고 있는 클라이언트가 없고, 좀비는 풀러 측에 남아있는 상태**임을 확인.

### 2. Supabase 대시보드 Roles 페이지 — 점 세 개 메뉴 ⚠️

`Database → Roles`에서 `postgres` 롤의 ⋮ 메뉴를 열었더니
`"Confirm to delete role 'postgres'"` 다이얼로그가 떴다.
**이건 세션 끊기가 아니라 롤 자체 삭제 메뉴**라 즉시 Cancel.
누르면 DB 메인 롤이 영구 삭제되어 서비스가 망가졌을 것.

> 교훈: Supabase Roles 페이지에는 "세션 끊기" 단일 액션 버튼이 없다.
> 풀러 세션 정리는 SQL로 한다.

### 3. SQL Editor로 pg_stat_activity 조회 — 확진

```sql
SELECT pid, usename, application_name, state, state_change
FROM pg_stat_activity
WHERE usename = 'postgres'
  AND pid <> pg_backend_pid()
ORDER BY state_change;
```

결과 15행, 전부 다음 패턴.

| application_name | state | 비고 |
|---|---|---|
| `PostgreSQL JDBC Driver` | `idle` | × 15 |

→ 좀비 JDBC 세션 15개가 한도를 정확히 점유 중임을 확인.

### 4. idle JDBC 세션 종료 시도 — 부분 효과

```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE usename = 'postgres'
  AND pid <> pg_backend_pid()
  AND application_name = 'PostgreSQL JDBC Driver'
  AND state = 'idle';
```

종료 직후 Roles 페이지의 connection 수가 도리어 **16 → 17로 증가**.
원인 후보:

- Roles 페이지 카운트가 캐시되어 즉시 반영되지 않음
- Supavisor가 종료된 백엔드 연결을 자동으로 재수립
- 종료 직후 다른 클라이언트(팀원 등)가 새 연결을 잡음

`pg_terminate_backend` 단독으로는 즉시·완전한 해소를 보장하기 어렵다는 점을 확인.

---

## 성공한 방법

`application-local.yaml`의 `spring.datasource` 블록을 다음과 같이 교체해서
**부팅 성공·재발 방지를 동시에** 잡았다. (확정일 2026-05-21)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:6543/postgres?pgbouncer=true&prepareThreshold=0
    username: 
    password:      # 로컬 개발 전용. 절대 운영 키를 넣지 말 것.
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5           # 기본 10 → 5
      minimum-idle: 2
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 600000
```

핵심은 세 가지를 한꺼번에 바꿨다는 점이다.

### 1. Transaction pooler 엔드포인트(포트 6543) 사용

- 호스트는 그대로(`aws-1-ap-northeast-2.pooler.supabase.com`), **포트만 5432 → 6543**.
- session mode의 `pool_size: 15` 제약에서 벗어나 transaction mode의 큰 동시
  한도(수백)를 사용한다. 한 번의 좀비 누적이 곧바로 부팅 실패로 이어지는 구조가
  사라진다.
- Supabase 대시보드 `Settings → Database → Connection string` 의
  **Transaction pooler** 탭에서 정식 URI를 받아 적용했다.

### 2. JDBC URL에 `pgbouncer=true&prepareThreshold=0` 추가

Transaction mode 풀러(Supavisor/PgBouncer 호환)는 **서버측 prepared statement**를
세션 간에 공유할 수 없다. 옵션을 끄지 않으면 `prepared statement … does not exist`
류 오류가 산발적으로 발생한다.

- `prepareThreshold=0` — 서버측 prepare 비활성화.
- `pgbouncer=true` — PgBouncer 호환 모드 강제.

### 3. HikariCP 풀 크기 5로 제한 + 라이프사이클 명시

```yaml
hikari:
  maximum-pool-size: 5
  minimum-idle: 2
  connection-timeout: 20000   # 20s
  idle-timeout: 300000        # 5min  (Supabase idle 종료보다 짧게)
  max-lifetime: 600000        # 10min (서버측 강제 종료 전에 갱신)
```

- `maximum-pool-size: 5` — 기본 10에서 줄여 한 인스턴스 점유 슬롯 축소.
- `idle-timeout`/`max-lifetime`을 풀러 측 idle 정리 주기보다 **짧게** 두어
  HikariCP가 좀비를 만들기 전에 먼저 끊고 재발급한다. 이게 좀비 누적의 1차 방어선.

### 결과

`./gradlew bootRun --args='--spring.profiles.active=local'` 정상 기동 확인.
Supabase Roles 페이지의 `postgres` 활성 connection 수는
인스턴스 한 개 기준 ~5 내외로 안정화.

---

## 검증

부팅 성공 검증은 다음으로 한다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
# Tomcat started on port 8080 / Started eqhApplication in X seconds 로그 확인
```

부팅 후 Supabase SQL Editor에서 현재 점유 슬롯 재확인:

```sql
SELECT count(*), state, application_name
FROM pg_stat_activity
WHERE usename = 'postgres'
GROUP BY state, application_name
ORDER BY count DESC;
```

`PostgreSQL JDBC Driver` 행이 HikariCP `maximum-pool-size` 설정값과 일치하는지 확인.

---

## 교훈 / 재발 방지

- **풀러 세션 모드(5432) + HikariCP 기본값(10)** 조합은 1인 개발에서도 한도(15)에
  쉽게 부딪힌다. 로컬 개발 yaml에는 풀 크기 명시를 기본으로 한다.
- Supabase Roles 페이지의 ⋮ 메뉴는 **세션 끊기가 아니라 롤 삭제**다. 절대 누르지 말 것.
  세션 정리는 항상 SQL Editor + `pg_terminate_backend`.
- `pg_terminate_backend`는 Supavisor 환경에선 즉효를 보장하지 않는다.
  카운트가 줄지 않으면 프로젝트 재시작이 가장 확실하다.
- 같은 Supabase 인스턴스를 팀이 공유하는 한 본 문제는 누구든 다시 만들 수 있다.
  중장기적으로 **로컬 개발용 Postgres(도커)** 분리 또는 **개발용 Supabase
  프로젝트 분리** 검토.
