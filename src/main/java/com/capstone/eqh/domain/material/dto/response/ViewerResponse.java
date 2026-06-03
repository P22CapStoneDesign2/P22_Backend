package com.capstone.eqh.domain.material.dto.response;

import com.capstone.eqh.domain.material.entity.Material;

public record ViewerResponse(
        Long materialId,
        String pdfUrl,
        int pageCount,
        String aspectRatio,
        boolean allowDownload
) {

    public static ViewerResponse from(Material material) {
        return new ViewerResponse(
                material.getId(),
                material.getPdfUrl(),
                material.getPageCount(),
                material.getAspectRatio(),
                material.isAllowDownload()
        );
    }
}
