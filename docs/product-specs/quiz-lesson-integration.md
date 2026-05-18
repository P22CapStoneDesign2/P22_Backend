# 퀴즈-교안 연동 스펙

> 상태: ✅ 구현됨

## 개요

교수가 퀴즈 문제를 생성할 때 해당 문제가 **어떤 교안의 몇 페이지, 몇 번째 문단**에서 왔는지 지정한다.
학생이 퀴즈를 제출한 후 오답을 확인할 때, 틀린 문제마다 교수가 지정한 **교안 참조(교안명, 페이지, 문단)**를 함께 제공한다.

## 사용자 시나리오

### 교수 (PROF)
1. 퀴즈 문제 생성 시 `anchorId`(교안 ID), `lessonPage`, `lessonParagraph` 선택적 지정
2. 지정하지 않아도 문제 생성 가능 (`anchorId` nullable)

### 학생 (USER)
1. 퀴즈 제출 (`POST /api/quiz/{quizId}/submit`)
2. 오답 목록 조회 (`GET /api/quiz/wrong-answers`)
3. 응답에서 `lessonRef` 확인 → 해당 교안의 해당 페이지로 이동

## 데이터 흐름

```
① PROF → 문제 생성: anchorId, lessonPage, lessonParagraph 지정
② USER → 퀴즈 제출 (POST /api/quiz/{quizId}/submit)
③ 서버 → correctAnswer와 studentAnswer 대소문자 무시 비교 자동 채점
④ 서버 → quiz_sub / quiz_sub_answer 저장
⑤ USER → 오답 조회 (GET /api/quiz/wrong-answers)
⑥ 응답 → 틀린 문제 + lessonRef { lessonId, lessonTitle, lessonPage, lessonParagraph }
```

## 수용 기준

- [x] `anchorId` 없이 문제 생성 가능 (nullable)
- [x] 오답 응답의 `lessonRef`는 `anchorId` 미지정 시 `null`
- [x] 채점은 대소문자 무시(`equalsIgnoreCase`) 비교
- [x] 퀴즈 1인당 1회만 제출 가능 — 재제출 시 409 반환
- [x] `WrongAnswerResponseDto`에 `lessonRef` 내부 클래스 포함

## 관련 코드

| 위치 | 역할 |
|------|------|
| `QuizQuestion.anchor` | FK → `lecture_material` |
| `QuizQuestion.lessonPage`, `.lessonParagraph` | 교수 지정 위치 |
| `QuizService.submit()` | 채점 로직 |
| `QuizSubmissionAnswerRepository.findWrongAnswersByStudentId()` | 오답 쿼리 |
| `WrongAnswerResponseDto.LessonRefDto` | 교안 참조 응답 |
