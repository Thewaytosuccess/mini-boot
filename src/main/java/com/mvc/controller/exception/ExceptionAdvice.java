package com.mvc.controller.exception;

import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.util.exception.ExceptionHandler;

/**
 * @author xhzy
 */
@ControllerAdvice(exceptionHandler = ExceptionAdvice.class)
public class ExceptionAdvice implements ExceptionHandler {

    @Override
    public Object handle(Exception e) {
        System.out.println("ha ha exception is caught");
        e.printStackTrace();
        return "ha ha exception is caught";
    }
}
