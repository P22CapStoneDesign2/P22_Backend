package com.capstone.eqh.domain.material.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProgressRequest(
        @NotNull(message = "현재 페이지는 필수입니다.")
        @Min(value = 1, message = "현재 페이지는 1 이상이어야 합니다.")
        Integer currentPage
) {
}
