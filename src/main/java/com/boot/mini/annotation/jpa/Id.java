package com.boot.mini.annotation.jpa;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
@Documented
public @interface Id {

    boolean autoIncrement() default true;

}
