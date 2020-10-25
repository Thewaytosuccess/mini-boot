package com.mvc.annotation.bean.ioc;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
@Inherited
public @interface Resource {

    String name() default "";

    Class<?> type() default Object.class;

}
