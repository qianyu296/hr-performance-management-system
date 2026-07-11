package com.hrpm.common.api;

public record ApiResponse<T>(String code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "ok", data, null);
    }
}
