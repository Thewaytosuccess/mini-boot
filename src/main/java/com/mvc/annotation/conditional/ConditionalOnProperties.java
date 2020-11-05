package com.mvc.annotation.conditional;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ConditionalOnProperties {

     String value();
}
