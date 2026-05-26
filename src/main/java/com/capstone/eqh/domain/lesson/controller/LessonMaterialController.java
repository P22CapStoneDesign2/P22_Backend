package com.capstone.eqh.domain.lesson.controller;

import com.capstone.eqh.domain.lesson.dto.request.LessonCreateRequestDto;
import com.capstone.eqh.domain.lesson.dto.request.LessonUpdateRequestDto;
import com.capstone.eqh.domain.lesson.dto.response.LessonMaterialResponseDto;
import com.capstone.eqh.domain.lesson.service.LessonMaterialService;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.global.common.ApiResponse;
import com.capstone.eqh.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lessons/{lessonId}/materials")
@RequiredArgsConstructor
public class LessonMaterialController {

    private final LessonMaterialService materialService;

    @PostMapping
    @PreAuthorize("hasRole('PROF') and principal.active and @lessonService.isOwner(#lessonId, principal.userId)")
    public ResponseEntity<ApiResponse<LessonMaterialResponseDto>> create(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LessonCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "교안 생성 성공",
                        materialService.create(lessonId, request, userDetails.getUserId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LessonMaterialResponseDto>>> getAll(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Role role = userDetails.getUser().getRole();
        return ResponseEntity.ok(ApiResponse.success(200, "교안 목록 조회 성공",
                materialService.getAll(lessonId, userDetails.getUserId(), role, pageable)));
    }

    @GetMapping("/{materialId}")
    public ResponseEntity<ApiResponse<LessonMaterialResponseDto>> getOne(
            @PathVariable Long lessonId,
            @PathVariable Long materialId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Role role = userDetails.getUser().getRole();
        return ResponseEntity.ok(ApiResponse.success(200, "교안 조회 성공",
                materialService.getOne(lessonId, materialId, userDetails.getUserId(), role)));
    }

    @PutMapping("/{materialId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @lessonMaterialService.isOwner(#materialId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LessonMaterialResponseDto>> update(
            @PathVariable Long lessonId,
            @PathVariable Long materialId,
            @Valid @RequestBody LessonUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(200, "교안 수정 성공",
                materialService.update(lessonId, materialId, request)));
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @lessonMaterialService.isOwner(#materialId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long lessonId,
            @PathVariable Long materialId) {
        materialService.delete(lessonId, materialId);
        return ResponseEntity.ok(ApiResponse.success("교안 삭제 성공"));
    }
}
