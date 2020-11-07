package com.mvc.annotation.jpa;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
@Documented
public @interface Column {

    String column() default "";

    int length() default 0;

    boolean nonnull() default false;

    String comment() default "";

    boolean unsigned() default false;

    String defaultValue() default "";

}
