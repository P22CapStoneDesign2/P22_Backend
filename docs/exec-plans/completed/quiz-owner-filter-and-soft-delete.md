# [완료] 퀴즈 — 교수 본인 필터 및 소프트 삭제 전환

- **시작일**: 2026-05-18
- **완료일**: 2026-05-18
- **브랜치**: feat/quiz_sik

## 목표

1. `GET /api/quiz` 가 모든 퀴즈를 반환하던 동작을 역할 기반으로 분기한다.
   - `PROF` — 본인이 생성한 퀴즈만
   - `USER`, `ADMIN` — 전체 (현행 유지)
2. `DELETE /api/quiz/{id}` 를 하드 삭제 → 소프트 삭제로 전환한다.
3. `@PreAuthorize` 거부 시 500 으로 떨어지던 응답을 403 으로 정상화한다.

## 배경 (실제 발생한 버그)

프론트에서 `GET /api/quiz/1/edit` 호출 시 HTTP **500 Internal Server Error** 발생.
원인 추적 결과 두 문제가 결합된 것으로 확인:

1. **목록에서 타 교수 퀴즈가 노출** — `GET /api/quiz` 가 모든 퀴즈를 반환하므로, A 교수의 화면에서 B 교수의 퀴즈 카드가 보이고 "수정" 버튼이 활성화됨. 클릭하면 `/api/quiz/{id}/edit` 가 호출되지만 `@PreAuthorize` 에서 거부됨.
2. **거부 응답 변환 누락** — `@PreAuthorize` 가 던지는 `AuthorizationDeniedException`(= `AccessDeniedException` 서브클래스) 이 `GlobalExceptionHandler.handleException(Exception.class)` catch-all 에 먼저 잡혀 500 으로 변환됨. `SecurityConfig.accessDeniedHandler` 는 필터 체인 레벨 거부 전용이라 이 경로에서는 발화하지 않음.

부수적으로, 강사가 본인이 만든 퀴즈를 잘못 삭제했을 때 복구할 수 있도록 삭제 정책도 소프트 삭제로 전환하기로 결정.

## 수용 기준

- [x] PROF 계정으로 `GET /api/quiz` 호출 시 본인 퀴즈만 반환된다
- [x] USER / ADMIN 은 기존과 동일하게 전체 퀴즈를 받는다 (단, 소프트 삭제분은 제외)
- [x] `DELETE /api/quiz/{id}` 호출 시 `quiz`, `quiz_q`, `quiz_opt` 모두 `deleted=true`, `deleted_at=NOW()` 로 UPDATE 되며 실제 row 는 보존된다
- [x] 소프트 삭제된 퀴즈는 `GET /api/quiz`, `GET /api/quiz/{id}`, `GET /api/quiz/{id}/edit` 모두에서 노출되지 않는다
- [x] 권한 부족 시 응답이 403 + `ApiResponse.failure(403, "접근 권한이 없습니다.")` 로 정상화된다
- [x] 학생 제출 이력 / 오답 노트는 보존된다 (단, 소프트 삭제된 문제는 오답 조회 결과에서 제외)
- [x] `./gradlew build` 통과 (ArchUnit 포함)

## 변경 대상 파일

### 엔티티 — 소프트 삭제 도입

| 파일 | 변경 내용 |
|------|----------|
| `domain/quiz/entity/Quiz.java` | `deleted`/`deletedAt` 필드, `@SQLDelete`(UPDATE), `@SQLRestriction`("deleted = false") 추가 |
| `domain/quiz/entity/QuizQuestion.java` | 동일 패턴 — 부모 quiz 가 cascade 로 소프트 삭제될 때 함께 마킹 |
| `domain/quiz/entity/QuizOption.java` | `@SQLDelete` 만 추가 (옵션은 question 통해서만 로드되므로 `@SQLRestriction` 불필요, 데이터 손실 방지 목적) |

### 조회 분기

| 파일 | 변경 내용 |
|------|----------|
| `domain/quiz/repository/QuizRepository.java` | `findByProfessor_Id(Long, Pageable)` 추가 |
| `domain/quiz/service/QuizService.java` | `getAll(userId, role, pageable)` — `PROF` 일 때 `findByProfessor_Id`, 아니면 `findAll` |
| `domain/quiz/controller/QuizController.java` | `getAll` 에서 `@AuthenticationPrincipal` 로 role 추출 후 service 에 전달 (도메인 layer 격리 유지 — `CLAUDE.md` 규칙 #2) |

### 예외 처리 정상화

| 파일 | 변경 내용 |
|------|----------|
| `global/exception/GlobalExceptionHandler.java` | `@ExceptionHandler(AccessDeniedException.class)` 추가 — `ErrorCode.FORBIDDEN` 매핑으로 403 반환 |

### 문서 갱신

| 파일 | 변경 내용 |
|------|----------|
| `docs/API.md` §16 | `GET /api/quiz` 역할별 노출 범위 명시 |
| `docs/API.md` §20 | `DELETE /api/quiz/{quizId}` 소프트 삭제 동작 명시 |
| `docs/generated/db-schema.md` | `quiz`, `quiz_q`, `quiz_opt` 에 `deleted`, `deleted_at` 컬럼 추가 |

### DB 마이그레이션

`application-local.yaml` 은 `ddl-auto: update` 라 로컬에서는 자동 반영.
**운영/스테이징은 수동 ALTER 필요**:

```sql
ALTER TABLE quiz     ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE, ADD COLUMN deleted_at DATETIME;
ALTER TABLE quiz_q   ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE, ADD COLUMN deleted_at DATETIME;
ALTER TABLE quiz_opt ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE, ADD COLUMN deleted_at DATETIME;
```

---

## 소프트 삭제 — 수동 필터 vs 자동 필터

이 작업에서 가장 중요한 설계 결정은 **소프트 삭제의 필터링 방식** 이다.
프로젝트에는 이미 `User` 엔티티가 수동 필터 방식으로 소프트 삭제를 구현해 두었지만,
이번 `Quiz` 작업에는 **자동 필터 방식 (Hibernate `@SQLDelete` + `@SQLRestriction`)** 을 택했다.

### 방식 1 — 수동 필터 (기존 `User` 엔티티)

엔티티에 `deleted` 컬럼만 두고, 조회/삭제 시 개발자가 직접 분기 코드를 작성.

```java
@Entity
public class User {
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}

// 조회 — 매 호출마다 명시적 필터
userRepository.findByEmail(email)
        .filter(u -> !u.isDeleted())
        .orElseThrow(...);

// 삭제 — softDelete() 직접 호출
user.softDelete();
```

**장점**
- 동작이 명시적 — 코드만 읽어도 어떤 쿼리가 삭제분을 포함/제외하는지 즉시 보임
- "삭제된 것도 포함해서 조회" 같은 예외 케이스가 쉬움 (필터를 안 걸기만 하면 됨)
- 어드민 화면·통계·복구 기능 구현에 유리
- JPA 표준 — 다른 ORM 으로 이식하기 쉬움

**단점**
- **누락 위험** — 조회 진입점이 늘수록 한 곳만 빠뜨려도 데이터 노출 버그
- `@OneToMany`, `@ManyToOne` 연관 로딩 시 필터 적용 불가능 — 콜렉션이 그대로 전부 로드됨
- 코드 리뷰 때 "여기 필터 빠진 거 아닌가?" 매번 확인 필요

### 방식 2 — 자동 필터 (이번 `Quiz` 엔티티)

Hibernate 가 DELETE SQL 을 UPDATE 로 바꿔치고, 모든 SELECT 에 WHERE 절을 자동 부착.

```java
@Entity
@SQLDelete(sql = "UPDATE quiz SET deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted = false")
public class Quiz {
    private boolean deleted = false;
    private LocalDateTime deletedAt;
}
```

| 어노테이션 | 역할 | 발화 시점 |
|---|---|---|
| `@SQLDelete` | `JpaRepository.delete()` 호출 시 실행되는 SQL 을 재정의 | 삭제 시 |
| `@SQLRestriction` | 이 엔티티의 모든 SELECT 에 WHERE 절 자동 부착 | findById, findAll, JPQL, 연관 로딩 등 모든 조회 |

실제 발생 SQL:

```sql
-- quizRepository.delete(quiz)
UPDATE quiz SET deleted = true, deleted_at = NOW() WHERE id = ?
-- cascade 로 자식들도 자동 처리
UPDATE quiz_q   SET deleted = true, deleted_at = NOW() WHERE id = ?
UPDATE quiz_opt SET deleted = true, deleted_at = NOW() WHERE id = ?

-- quizRepository.findById(1L)
SELECT * FROM quiz WHERE id = ? AND deleted = false
--                                   ↑↑↑↑↑↑↑↑↑↑↑↑↑ 자동 부착

-- quiz.getQuestions() (lazy load)
SELECT * FROM quiz_q WHERE quiz_id = ? AND deleted = false
```

**장점**
- **누락이 불가능** — 새 리포지토리 메서드를 추가해도, JPQL 을 짜도, lazy 로딩이 일어나도 모두 필터됨
- cascade 한 줄로 트리 전체 소프트 삭제 가능 (`quiz → quiz_q → quiz_opt`)
- 비즈니스 로직 코드가 깨끗 — `quizRepository.delete()` 만 호출하면 됨

**단점**
- **삭제된 행을 일부러 조회하기 어렵다** — 어드민 복구 기능을 만들려면 native query 나 `@FilterDef` 로 우회해야 함
- "왜 데이터가 없지?" 디버깅 시 한 단계 함정 — SQL 로그를 봐야 `AND deleted = false` 부착 여부 확인 가능
- **Hibernate 전용 기능** — JPA 표준이 아니므로 다른 ORM 이식 시 재작업 필요

### 자동 필터를 선택한 이유

| 기준 | `User` (수동 채택) | `Quiz` (자동 채택) |
|---|---|---|
| 조회 진입점 수 | 적음 (`findByEmail`, `findById`) | 많음 (`findAll`, `findById`, `findByProfessor_Id`, OneToMany 연관 로딩 등) |
| cascade 트리 깊이 | 단일 테이블 | 3단 (`quiz → quiz_q → quiz_opt`) |
| "삭제된 것도 조회" 요구 | 있을 수 있음 (어드민 회원 관리) | 없음 |
| 위험의 비대칭성 | 필터 누락 시 단일 row 노출 | 필터 누락 시 다른 교수의 문제 / 정답 / 해설 누출 가능 |

**핵심 결정 근거**: `Quiz` 는 cascade 가 깊고 조회 경로가 많아, 수동 필터 방식으로는 *언젠가 반드시* 한 곳에서 누락이 발생한다.
누락 시 노출되는 데이터가 다른 교수의 문제 은행과 정답이므로, 비대칭 위험을 자동 필터로 막는 게 합리적이다.
복구 기능은 현재 요구사항에 없고, 필요 시 native query 로 우회 가능하다는 점에서 "기능 손실" 도 작다.

---

## 알고 있지만 선택하지 않은 기술 (의사결정 근거 보존용)

이 섹션은 "이런 대안도 검토했으나 채택하지 않았다" 를 보존하기 위함.

### A. JPA Specification + 동적 `@Query` 로 매번 `deleted = false` 추가

```java
@Query("SELECT q FROM Quiz q WHERE q.deleted = false")
Page<Quiz> findAllActive(Pageable pageable);
```

- **장점**: JPA 표준, 명시적
- **단점**: 모든 리포지토리 메서드를 손으로 작성해야 하고, OneToMany 연관 로딩에는 적용 불가 — 수동 필터의 단점을 그대로 가짐
- **기각 이유**: 수동 필터 + 가독성만 떨어짐. 누락 위험 동일

### B. Hibernate `@FilterDef` + `@Filter`

```java
@Entity
@FilterDef(name = "activeOnly", defaultCondition = "deleted = false")
@Filter(name = "activeOnly")
public class Quiz { ... }

// 활성화/비활성화 가능
entityManager.unwrap(Session.class).enableFilter("activeOnly");
```

- **장점**: 자동 필터의 장점을 그대로 가지면서, 런타임에 필터를 끄고 켤 수 있음 → 어드민에서 "삭제분 포함 조회" 가능
- **단점**: `@SQLRestriction` 보다 설정 코드가 길고, 매 트랜잭션마다 `enableFilter` 호출 필요. 누락 시 필터가 꺼진 채로 운영되는 사고 가능
- **기각 이유**: 어드민 복구 기능이 요구사항에 없음. 필요해지는 시점에 도입해도 늦지 않음 (YAGNI)

### C. Spring Data JPA `@Query(nativeQuery=true)` + view 테이블 분리

- **장점**: 활성 데이터와 삭제 데이터를 view 로 완전히 분리 → 가장 안전
- **단점**: 인프라 복잡도 ↑ (view 정의, 마이그레이션 스크립트, view 권한 등)
- **기각 이유**: 학부 캡스톤 규모에서 과한 설계

### D. 도메인 이벤트 + 소프트 삭제 핸들러 (DDD 스타일)

```java
public void delete() {
    registerEvent(new QuizDeletedEvent(this.id));
    // listener 에서 deleted=true 마킹
}
```

- **장점**: 도메인 로직과 영속성 관심사가 분리됨, 감사 로그·알림 등 부수 로직 확장 용이
- **단점**: 이벤트 발행/구독 인프라 추가 필요. 단순 소프트 삭제에는 과도
- **기각 이유**: 현재 요구사항에 부수 로직 없음. 미래에 감사 로그가 필요해지면 도입 검토

### E. `AccessDeniedException` → Spring 의 `accessDeniedHandler` 로 처리

문제가 된 500 응답 케이스에 대해, `GlobalExceptionHandler` 에 핸들러를 추가하는 대신 `ExceptionTranslationFilter` 가 `accessDeniedHandler` 로 라우팅하도록 유도하는 방법도 있다.

- **방법**: `GlobalExceptionHandler.handleException(Exception)` 의 catch-all 에서 `AccessDeniedException` 만 재던지기 → `DispatcherServlet` 이 처리하지 않으면 `ExceptionTranslationFilter` 가 잡음
- **단점**: catch-all 의 의미가 모호해지고, 한 가지 예외만 특별 취급한다는 게 코드만 봐서는 명확하지 않음
- **기각 이유**: `@ExceptionHandler(AccessDeniedException.class)` 를 직접 두는 게 의도가 명시적이고, 응답 envelope 도 일관됨 (`ApiResponse.failure`)

---

## 의사결정 로그

- **2026-05-18** — `AccessDeniedException` 전용 `@ExceptionHandler` 추가
  - 이유: `@PreAuthorize` 거부 시 `AuthorizationDeniedException`(= `AccessDeniedException` 서브클래스)이 `Exception.class` catch-all 에 먼저 잡혀 500 으로 변환되던 문제 해결
  - 대안 검토: `SecurityConfig.accessDeniedHandler` 를 활용하려면 catch-all 에서 재던지기 필요 → 코드 의도 불명확하여 기각 (위 E 항목)

- **2026-05-18** — `GET /api/quiz` 역할 분기 — PROF 만 본인 필터
  - 이유: 사용자 시나리오상 학생은 어떤 교수의 퀴즈도 풀 수 있어야 하나, 교수는 자신의 문제 은행만 관리하므로 본인 것만 보면 충분. ADMIN 은 전체 운영 권한이 필요
  - 대안 검토: USER 도 본인 제출 이력 기반으로 필터하는 안 → 현행 PRD 에 명시 없고 별도 진입 흐름이 필요해 보류
  - 도메인 분리 준수: 컨트롤러에서 `@AuthenticationPrincipal` 로 role 만 추출해 service 에 전달 (CLAUDE.md 규칙 #2)

- **2026-05-18** — Quiz 트리에 Hibernate 자동 필터(`@SQLDelete` + `@SQLRestriction`) 채택
  - 이유: 위 "자동 필터를 선택한 이유" 표 참조. 조회 진입점·cascade 깊이 / 데이터 노출 비대칭성 / 복구 요구 없음
  - 대안 검토: 수동 필터(User 와 컨벤션 통일), `@FilterDef`, native query view 모두 검토 후 기각 (위 A·B·C 항목)

- **2026-05-18** — `QuizOption` 에는 `@SQLDelete` 만 적용, `@SQLRestriction` 미적용
  - 이유: 옵션은 항상 question 을 통해 로드되므로 question 이 가려지면 옵션도 자연히 가려짐. `@SQLRestriction` 을 굳이 안 붙여도 사용자 노출 위험 없음
  - 단, `@SQLDelete` 는 필요 — cascade 시 default 동작인 DELETE 가 발화하면 데이터가 실제로 사라짐. 보존 목적으로 UPDATE 로 재정의

- **2026-05-18** — 학생 제출/오답 이력(`quiz_submission`, `quiz_submission_answer`)은 소프트 삭제 대상에서 제외
  - 이유: 학생 오답 노트가 끊기지 않아야 함. 단, 소프트 삭제된 문제는 `QuizQuestion.@SQLRestriction` 때문에 오답 조회 결과에서 자동 제외됨 — 사용자에게 이 트레이드오프 사전 동의 받음

---

## 면접 답변용 요약

> **Q. 알고는 있지만 이번 프로젝트에서 구현하지 않은 기술이 있다면?**
>
> 소프트 삭제 구현에서 Hibernate `@FilterDef` + `@Filter` 방식과 도메인 이벤트 기반 삭제 처리 방식을 검토했지만 채택하지 않았습니다.
>
> `@FilterDef` 는 런타임에 필터를 켜고 끌 수 있어서 어드민 화면에서 "삭제된 데이터 포함 조회" 같은 기능을 깔끔하게 구현할 수 있지만, 현재 요구사항에 복구 기능이 없어서 `@SQLRestriction` 으로 항상 켜진 필터를 두는 게 안전성·단순성 면에서 더 낫다고 판단했습니다. 필요해지는 시점에 도입해도 늦지 않다는 YAGNI 원칙입니다.
>
> 도메인 이벤트 방식은 감사 로그나 알림 같은 부수 로직 확장에 유리하지만, 단순 소프트 삭제 한 가지를 위해 이벤트 발행/구독 인프라를 도입하는 건 과도하다고 봤습니다.
>
> 또 같은 프로젝트 안의 `User` 엔티티는 수동 필터(`.filter(u -> !u.isDeleted())`) 방식을 쓰는데, `Quiz` 는 자동 필터(`@SQLDelete` + `@SQLRestriction`) 방식으로 다르게 갔습니다. `User` 는 조회 진입점이 적고 어드민에서 탈퇴 회원을 봐야 할 가능성이 있어 수동이 맞고, `Quiz` 는 cascade 가 3단이고 조회 경로가 많아 누락 위험을 자동 필터로 봉쇄하는 게 맞다고 판단해서 의도적으로 컨벤션을 다르게 두었습니다.
