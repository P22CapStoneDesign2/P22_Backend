# 교안 PDF 업로드·삭제 — 실행 계획

## 상태

- [x] Entity `LessonPdf`, `Lesson` 연관관계
- [x] `LessonPdfRepository`, `LessonPdfService`, `LessonPdfController`
- [x] `SupabaseStorageService` + `SupabaseStorageProperties`
- [x] `ErrorCode` (PDF_NOT_FOUND, INVALID_PDF_TYPE, FILE_SIZE_EXCEEDED, STORAGE_*)
- [x] `docs/API.md` §13-1
- [x] `application.example.yaml` Supabase·multipart 설정 예시

## 환경 변수

| 변수 | 설명 |
|------|------|
| `SUPABASE_URL` | 프로젝트 URL (예: `https://xxx.supabase.co`) |
| `SUPABASE_SERVICE_KEY` | service_role 키 (Storage API) |
| `SUPABASE_BUCKET` | 버킷명 (기본 `lesson-pdf`) |

## 테스트 케이스 (수동)

| # | 시나리오 | 기대 |
|---|----------|------|
| 1 | PROF(교안 소유자) PDF 업로드 | 201, metadata·URL 반환 |
| 2 | USER PDF 업로드 | 403 |
| 3 | 비 PDF / MIME 오류 | 400 INVALID_PDF_TYPE |
| 4 | 50MB 초과 | 400 FILE_SIZE_EXCEEDED |
| 5 | PDF 목록 GET | 200, 배열 |
| 6 | 업로더 PROF PDF 삭제 | 200 |
| 7 | 타 PROF PDF 삭제 | 403 |
| 8 | ADMIN PDF 삭제 | 200 |
