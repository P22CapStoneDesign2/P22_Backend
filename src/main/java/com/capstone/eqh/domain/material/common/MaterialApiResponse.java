package com.capstone.eqh.domain.material.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaterialApiResponse<T>(
        boolean success,
        String errorCode,
        String message,
        T data
) {

    public static <T> MaterialApiResponse<T> ok(T data) {
        return new MaterialApiResponse<>(true, null, null, data);
    }

    public static MaterialApiResponse<Void> okMessage(String message) {
        return new MaterialApiResponse<>(true, null, message, null);
    }

    public static <T> MaterialApiResponse<T> fail(String errorCode, String message) {
        return new MaterialApiResponse<>(false, errorCode, message, null);
    }
}
