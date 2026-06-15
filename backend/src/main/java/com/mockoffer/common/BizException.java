package com.mockoffer.common;

/** 业务异常：携带统一错误码，由 GlobalExceptionHandler 转成 ApiResponse。 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
