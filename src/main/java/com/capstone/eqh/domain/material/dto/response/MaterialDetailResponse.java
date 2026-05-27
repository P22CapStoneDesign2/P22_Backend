package com.capstone.eqh.domain.material.dto.response;

import com.capstone.eqh.domain.material.entity.Material;

import java.time.LocalDateTime;

public record MaterialDetailResponse(
        Long materialId,
        String title,
        String description,
        int pageCount,
        String aspectRatio,
        String createdBy,
        LocalDateTime createdAt
) {

    public static MaterialDetailResponse from(Material material) {
        return new MaterialDetailResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getPageCount(),
                material.getAspectRatio(),
                material.getUploadedBy().getNickname(),
                material.getCreatedAt()
        );
    }
}
