package com.boot.mini.annotation.conditional;

import com.boot.mini.core.conditional.ConditionalClass;

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
