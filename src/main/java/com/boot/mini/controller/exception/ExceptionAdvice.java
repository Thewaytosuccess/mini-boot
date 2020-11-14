package com.boot.mini.controller.exception;

import com.boot.mini.core.exception.ExceptionWrapper;
import com.boot.mini.core.exception.ExceptionHandler;

/**
 * @author xhzy
 */
//@ControllerAdvice(exceptionHandler = ExceptionAdvice.class)
public class ExceptionAdvice implements ExceptionHandler {

    @Override
    public Object handle(Exception e) {
        System.out.println("exception ==="+e.getMessage());
        if(e instanceof ExceptionWrapper){
            return ((ExceptionWrapper)e).toJson();
        }else{
            return new ExceptionWrapper(e).toJson();
        }
    }
}
