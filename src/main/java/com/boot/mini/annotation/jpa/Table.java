package com.boot.mini.annotation.jpa;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Table {

   String table() default "";

   boolean create() default false;

   String comment() default "";
}
