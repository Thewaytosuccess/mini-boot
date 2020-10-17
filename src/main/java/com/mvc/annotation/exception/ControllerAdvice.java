package com.mvc.annotation.exception;

import com.mvc.util.exception.ExceptionHandler;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface ControllerAdvice {

    String value() default "";

    Class<? extends ExceptionHandler> exceptionHandler() default ExceptionHandler.class;

}
