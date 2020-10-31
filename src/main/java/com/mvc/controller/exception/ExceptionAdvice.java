package com.mvc.controller.exception;

import com.mvc.core.exception.ExceptionHandler;
import com.mvc.core.exception.ExceptionWrapper;

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
