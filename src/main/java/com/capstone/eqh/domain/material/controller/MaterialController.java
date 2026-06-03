package com.capstone.eqh.domain.material.controller;

import com.capstone.eqh.domain.material.common.MaterialApiResponse;
import com.capstone.eqh.domain.material.dto.request.ProgressRequest;
import com.capstone.eqh.domain.material.dto.response.MaterialDetailResponse;
import com.capstone.eqh.domain.material.dto.response.MaterialListResponse;
import com.capstone.eqh.domain.material.dto.response.PageResponse;
import com.capstone.eqh.domain.material.dto.response.ViewerResponse;
import com.capstone.eqh.domain.material.service.MaterialService;
import com.capstone.eqh.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping("/api/lectures/{lectureId}/materials")
    public ResponseEntity<MaterialApiResponse<List<MaterialListResponse>>> listByLecture(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MaterialListResponse> data = materialService.getMaterialsByLecture(
                lectureId,
                userDetails.getUserId(),
                userDetails.getUser().getRole());
        return ResponseEntity.ok(MaterialApiResponse.ok(data));
    }

    @GetMapping("/api/materials/{materialId}")
    public ResponseEntity<MaterialApiResponse<MaterialDetailResponse>> getDetail(
            @PathVariable Long materialId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MaterialDetailResponse data = materialService.getMaterialDetail(
                materialId,
                userDetails.getUserId(),
                userDetails.getUser().getRole());
        return ResponseEntity.ok(MaterialApiResponse.ok(data));
    }

    @GetMapping("/api/materials/{materialId}/viewer")
    public ResponseEntity<MaterialApiResponse<ViewerResponse>> getViewer(
            @PathVariable Long materialId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ViewerResponse data = materialService.getViewer(
                materialId,
                userDetails.getUserId(),
                userDetails.getUser().getRole());
        return ResponseEntity.ok(MaterialApiResponse.ok(data));
    }

    @GetMapping("/api/materials/{materialId}/pages/{pageNumber}")
    public ResponseEntity<MaterialApiResponse<PageResponse>> getPage(
            @PathVariable Long materialId,
            @PathVariable int pageNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageResponse data = materialService.getPage(
                materialId,
                pageNumber,
                userDetails.getUserId(),
                userDetails.getUser().getRole());
        return ResponseEntity.ok(MaterialApiResponse.ok(data));
    }

    @PostMapping("/api/materials/{materialId}/progress")
    public ResponseEntity<MaterialApiResponse<Void>> saveProgress(
            @PathVariable Long materialId,
            @Valid @RequestBody ProgressRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        materialService.saveProgress(
                materialId,
                request,
                userDetails.getUserId(),
                userDetails.getUser().getRole());
        return ResponseEntity.ok(MaterialApiResponse.okMessage("읽기 진행도가 저장되었습니다."));
    }
}
