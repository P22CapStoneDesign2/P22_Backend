package com.capstone.eqh.domain.material.exception;

import lombok.Getter;

/**
 * PDF 교안 뷰어 접근 권한이 없을 때 사용합니다.
 * Spring Security의 {@link org.springframework.security.access.AccessDeniedException}과 구분하기 위해 별도 타입을 둡니다.
 */
@Getter
public class MaterialAccessDeniedException extends RuntimeException {

    public static final String ERROR_CODE = "ACCESS_DENIED";
    public static final String DEFAULT_MESSAGE = "해당 교안에 접근할 권한이 없습니다.";

    public MaterialAccessDeniedException() {
        super(DEFAULT_MESSAGE);
    }
}
