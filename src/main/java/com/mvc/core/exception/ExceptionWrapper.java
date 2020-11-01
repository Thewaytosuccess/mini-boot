package com.mvc.core.exception;

import com.alibaba.fastjson.JSONObject;
import com.mvc.enums.ExceptionEnum;

/**
 * @author xhzy
 */
public class ExceptionWrapper extends RuntimeException {

    private final String code;

    private final String message;

    public ExceptionWrapper(ExceptionEnum exceptionEnum) {
        this.code = exceptionEnum.getCode();
        this.message = exceptionEnum.getMessage();
    }

    public ExceptionWrapper(Exception e) {
        this.code = ExceptionEnum.UNKNOWN_ERROR.getCode();
        this.message = e.toString();
    }

    public String getCode() {
        return code;
    }

    public String getSimpleMessage() {
        return message;
    }

    public JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code",this.code);
        jsonObject.put("message",this.message);
        return jsonObject;
    }

    @Override
    public String toString() {
        return "ExceptionWrapper{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}