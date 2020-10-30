package com.mvc.annotation.method.schedule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scheduled {

    String cron();

    String name() default "DEFAULT_SCHEDULE_";

    int delay() default 0;

    int count() default -1;

    int priority() default 0;

    String startAt() default "";

    String endAt() default "";

    String startAtPattern() default "yyyy-MM-dd HH:mm:ss";

    String endAtPattern() default "yyyy-MM-dd HH:mm:ss";

}
