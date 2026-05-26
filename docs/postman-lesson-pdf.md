# Postman — 교안 PDF API

## 환경 변수

| 변수 | 예시 |
|------|------|
| `baseUrl` | `http://localhost:8080` |
| `accessToken` | PROF 또는 ADMIN JWT |

## 공통 Header

```
Authorization: Bearer {{accessToken}}
```

---

## 1. PDF 업로드

- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/lessons/1/pdf`
- **Body:** `form-data`
  - Key: `file` (Type: **File**)
  - Value: 로컬 `.pdf` 파일 선택

**성공 (201):**

```json
{
  "status": 201,
  "message": "PDF 업로드 성공",
  "data": {
    "pdfId": 1,
    "lessonId": 1,
    "originalFileName": "1주차.pdf",
    "savedFileName": "uuid.pdf",
    "fileUrl": "https://...",
    "fileSize": 12345,
    "uploadedById": 1,
    "uploadedAt": "2026-05-22T14:30:12"
  }
}
```

---

## 2. PDF 목록 조회

- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/lessons/1/pdf`
- **Header:** `Authorization: Bearer {{accessToken}}`

---

## 3. PDF 삭제

- **Method:** `DELETE`
- **URL:** `{{baseUrl}}/api/lessons/pdf/1`
- **Header:** `Authorization: Bearer {{accessToken}}` (업로드한 PROF 또는 ADMIN)

**성공 (200):**

```json
{
  "status": 200,
  "message": "PDF 삭제 성공",
  "data": null
}
```

---

## 사전 준비

1. `application.yaml`에 DB·JWT·Supabase 설정 (`application.example.yaml` 참고)
2. Supabase Storage 버킷 `lesson-pdf` 생성 및 **public** 읽기 정책(또는 signed URL 정책에 맞게 `getPublicUrl` 조정)
3. PROF 로그인 후 `accessToken` 저장
4. `POST /api/lessons`로 교안 생성 후 `lessonId` 확인
