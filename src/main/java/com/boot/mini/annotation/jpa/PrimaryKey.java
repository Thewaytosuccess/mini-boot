package com.boot.mini.annotation.jpa;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
@Documented
public @interface PrimaryKey {

    Class<?> type() default String.class;

    Class<?> idGenerator() default Void.class;

    String policy() default "random";

}
