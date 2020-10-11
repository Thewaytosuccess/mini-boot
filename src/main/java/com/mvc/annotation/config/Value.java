package com.mvc.annotation.config;

import java.lang.annotation.*;

/**
 *
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
@Documented
public @interface Value {

    String value();
}
