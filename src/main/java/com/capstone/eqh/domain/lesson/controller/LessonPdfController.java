package com.capstone.eqh.domain.lesson.controller;

import com.capstone.eqh.domain.lesson.dto.response.LessonPdfResponseDto;
import com.capstone.eqh.domain.lesson.service.LessonPdfService;
import com.capstone.eqh.global.common.ApiResponse;
import com.capstone.eqh.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonPdfController {

    private final LessonPdfService lessonPdfService;

    @PostMapping(value = "/{lessonId}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("(hasRole('PROF') and @lessonService.isOwner(#lessonId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LessonPdfResponseDto>> uploadPdf(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title) { 
        LessonPdfResponseDto response = lessonPdfService.uploadPdf(
                lessonId, file, userDetails.getUserId(), title);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "PDF 업로드 성공", response));
    }

    @GetMapping("/{lessonId}/pdf")
    public ResponseEntity<ApiResponse<List<LessonPdfResponseDto>>> getPdfList(@PathVariable Long lessonId) {
        return ResponseEntity.ok(
                ApiResponse.success(200, "PDF 목록 조회 성공", lessonPdfService.getPdfList(lessonId)));
    }

    @DeleteMapping("/pdf/{pdfId}")
    @PreAuthorize("(hasRole('PROF') and @lessonPdfService.isUploader(#pdfId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePdf(@PathVariable Long pdfId) {
        lessonPdfService.deletePdf(pdfId);
        return ResponseEntity.ok(ApiResponse.success("PDF 삭제 성공"));
    }
}
