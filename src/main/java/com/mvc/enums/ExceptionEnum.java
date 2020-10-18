package com.mvc.enums;

/**
 * @author xhzy
 */

public enum ExceptionEnum {
    /**
     * 异常枚举
     */
    ILLEGAL_ARGUMENT("illegal_argument","参数非法"),
    UNKNOWN_ERROR("unknown_error","未知异常");

    private final String code;

    private final String message;

    ExceptionEnum(String code, String message){
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
