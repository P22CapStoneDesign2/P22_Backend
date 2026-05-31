package com.capstone.eqh.domain.material.dto.response;

import com.capstone.eqh.domain.material.entity.Material;

import java.time.LocalDateTime;

public record MaterialListResponse(
        Long materialId,
        String title,
        int pageCount,
        String thumbnailUrl,
        LocalDateTime uploadedAt
) {

    public static MaterialListResponse from(Material material, String thumbnailUrl) {
        return new MaterialListResponse(
                material.getId(),
                material.getTitle(),
                material.getPageCount(),
                thumbnailUrl,
                material.getCreatedAt()
        );
    }
}
