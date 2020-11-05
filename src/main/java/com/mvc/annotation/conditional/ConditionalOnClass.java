package com.mvc.annotation.conditional;

import com.mvc.core.conditional.ConditionalClass;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ConditionalOnClass {

    Class<? extends ConditionalClass> condition();

}
