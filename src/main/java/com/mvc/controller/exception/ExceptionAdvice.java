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
        return null;
    }
}
