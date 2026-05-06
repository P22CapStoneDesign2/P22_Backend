package com.capstone.eqh.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

// API 응답
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T> (
        int status,
        String message,
        T data
) { //controller 에서 사용
    //성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    //성공 응답 (메세지만)
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<> (200, message, null);
    }
    //실패 응답
    public static <T> ApiResponse<T> failure(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
