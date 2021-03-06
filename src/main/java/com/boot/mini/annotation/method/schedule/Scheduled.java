package com.boot.mini.annotation.method.schedule;

import com.boot.mini.core.task.schedule.config.DefaultScheduleConfig;
import com.boot.mini.core.task.schedule.config.ScheduleConfigAdapter;

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

    String cron() default "";

    String name() default "";

    String delay() default "";

    String priority() default "";

    String startAt() default "";

    String endAt() default "";

    String startAtPattern() default "yyyy-MM-dd HH:mm:ss";

    String endAtPattern() default "yyyy-MM-dd HH:mm:ss";

    String jobName() default "";

    String jobGroup() default "";

    String triggerName() default "";

    String triggerGroup() default "";

    String prefix() default "";

    Class<? extends ScheduleConfigAdapter> configClass() default DefaultScheduleConfig.class;

}
