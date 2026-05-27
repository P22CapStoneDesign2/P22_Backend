package com.capstone.eqh.domain.material.service;

import com.capstone.eqh.domain.material.entity.Material;
import com.capstone.eqh.global.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PDF 페이지 이미지가 Supabase에 {@code {storagePath}/pages/{pageNumber}.png} 경로로 저장된다고 가정합니다.
 */
@Component
@RequiredArgsConstructor
public class MaterialPageUrlResolver {

    private final SupabaseStorageService supabaseStorageService;

    public String resolvePageImageUrl(Material material, int pageNumber) {
        String pageStoragePath = material.getStoragePath() + "/pages/" + pageNumber + ".png";
        return supabaseStorageService.getPublicUrl(pageStoragePath);
    }
}
