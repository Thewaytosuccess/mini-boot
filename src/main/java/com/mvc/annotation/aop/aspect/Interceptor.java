package com.mvc.annotation.aop.aspect;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface Interceptor {

    int order() default 0;

    String[] excludes() default {};

}
