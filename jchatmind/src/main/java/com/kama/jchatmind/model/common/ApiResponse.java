package com.kama.jchatmind.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * 通用 API 响应包装对象。
 *
 * @param <T> 业务数据类型
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    /**
     * 构造响应对象。
     *
     * @param code 业务状态码
     * @param message 提示信息
     * @param data 业务数据
     */
    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 返回携带数据的成功响应。
     *
     * @param data 业务数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiCode.SUCCESS.code, ApiCode.SUCCESS.message, data);
    }

    /**
     * 返回不携带数据的成功响应。
     *
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ApiCode.SUCCESS.code, ApiCode.SUCCESS.message, null);
    }

    /**
     * 返回自定义成功提示的响应。
     *
     * @param data 业务数据
     * @param message 自定义成功提示
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(ApiCode.SUCCESS.code, message, data);
    }

    /**
     * 返回指定业务码的错误响应。
     *
     * @param code 业务码
     * @param message 错误信息
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(ApiCode code, String message) {
        return new ApiResponse<>(code.getCode(), message, null);
    }

    /**
     * 返回默认错误码的错误响应。
     *
     * @param message 错误信息
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ApiCode.ERROR.getCode(), message, null);
    }

    /**
     * API 业务码枚举。
     */
    @Getter
    @AllArgsConstructor
    public enum ApiCode {
        SUCCESS(200, "success"),
        ERROR(500, "error");

        private final int code;
        private final String message;

        /**
         * 根据整型状态码解析枚举。
         *
         * @param code 业务状态码
         * @return 对应枚举
         */
        public static ApiCode fromCode(int code) {
            for (ApiCode value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid code: " + code);
        }
    }
}
