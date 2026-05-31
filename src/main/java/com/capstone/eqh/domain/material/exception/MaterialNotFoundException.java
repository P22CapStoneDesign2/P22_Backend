package com.capstone.eqh.domain.material.exception;

import lombok.Getter;

@Getter
public class MaterialNotFoundException extends RuntimeException {

    public static final String ERROR_CODE = "MATERIAL_NOT_FOUND";

    public MaterialNotFoundException() {
        super("존재하지 않는 교안입니다.");
    }
}
