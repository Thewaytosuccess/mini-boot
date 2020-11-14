package com.boot.mini.enums;

/**
 * @author xhzy
 */

public enum HttpMethodEnum {
    /**
     * HTTP REQUEST TYPE
     */
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    OPTIONS("OPTIONS");

    private final String method;

    HttpMethodEnum(String method){
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
