package com.capstone.eqh.domain.lesson.controller;

import com.capstone.eqh.domain.lesson.dto.request.LessonCreateRequestDto;
import com.capstone.eqh.domain.lesson.dto.request.LessonUpdateRequestDto;
import com.capstone.eqh.domain.lesson.dto.response.LessonResponseDto;
import com.capstone.eqh.domain.lesson.service.LessonService;
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
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @PreAuthorize("hasRole('PROF') and principal.active")
    public ResponseEntity<ApiResponse<LessonResponseDto>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LessonCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "강의 생성 성공",
                        lessonService.create(request, userDetails.getUserId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LessonResponseDto>>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Role role = userDetails.getUser().getRole();
        return ResponseEntity.ok(ApiResponse.success(200, "강의 목록 조회 성공",
                lessonService.getAll(userDetails.getUserId(), role, pageable)));
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponseDto>> getOne(@PathVariable Long lessonId) {
        return ResponseEntity.ok(ApiResponse.success(200, "강의 조회 성공",
                lessonService.getOne(lessonId)));
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @lessonService.isOwner(#lessonId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponseDto>> update(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(200, "강의 수정 성공",
                lessonService.update(lessonId, request)));
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @lessonService.isOwner(#lessonId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long lessonId) {
        lessonService.delete(lessonId);
        return ResponseEntity.ok(ApiResponse.success("강의 삭제 성공"));
    }
}
