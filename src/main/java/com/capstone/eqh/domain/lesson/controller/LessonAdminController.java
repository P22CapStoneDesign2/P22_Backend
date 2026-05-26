package com.capstone.eqh.domain.lesson.controller;

import com.capstone.eqh.domain.lesson.dto.response.LessonResponseDto;
import com.capstone.eqh.domain.lesson.service.LessonService;
import com.capstone.eqh.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/lessons")
@RequiredArgsConstructor
public class LessonAdminController {

    private final LessonService lessonService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LessonResponseDto>>> getAllForAdmin(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "전체 강의 목록 조회 성공", lessonService.getAll(pageable)));
    }
}
