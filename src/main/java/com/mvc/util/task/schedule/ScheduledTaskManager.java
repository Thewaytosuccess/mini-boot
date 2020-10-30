package com.mvc.util.task.schedule;

import com.mvc.annotation.enable.EnableScheduling;
import com.mvc.annotation.method.schedule.Scheduled;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.util.DateUtil;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.injection.IocContainer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class ScheduledTaskManager {

    private static final ScheduledTaskManager MANAGER = new ScheduledTaskManager();

    private ScheduledTaskManager(){}

    public static ScheduledTaskManager getInstance(){ return MANAGER; }

    private Set<Method> tasks;

    public void scan(){
        if(Objects.isNull(tasks)){
            tasks = new HashSet<>();
        }

        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        boolean global = classes.stream().anyMatch(e -> e.isAnnotationPresent(SpringBootApplication.class) &&
                e.isAnnotationPresent(EnableScheduling.class));
        if(!global){
            classes = classes.stream().filter(e -> e.isAnnotationPresent(EnableScheduling.class)).collect(Collectors.toList());
        }
        classes.forEach(e -> tasks.addAll(Arrays.stream(e.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Scheduled.class))
                .collect(Collectors.toSet())));

        if(!tasks.isEmpty()){
            start();
        }
    }

    /**
     * todo 使用xxl-job/elastic-job执行定时任务
     */
    private void start(){
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
        AtomicInteger count = new AtomicInteger();

        String startAt;
        String endAt;
        String pattern;
        int priority;
        int delay;
        int index;

        TriggerBuilder<Trigger> triggerBuilder;
        CronTrigger trigger;
        DefaultJob job;
        Scheduler scheduler;
        JobDetail jobDetail;
        Scheduled scheduled;

        for (Method m:tasks){
            index = count.incrementAndGet();
            triggerBuilder = TriggerBuilder.newTrigger().withIdentity("TRIGGER_" + index, "TRIGGER_GROUP_" + index);
            scheduled = m.getAnnotation(Scheduled.class);
            startAt = scheduled.startAt();
            pattern = scheduled.startAtPattern();
            if(!startAt.isEmpty()){
                triggerBuilder.startAt(DateUtil.parse(startAt,pattern));
            }else{
                triggerBuilder.startNow();
            }

            endAt = scheduled.endAt();
            pattern = scheduled.endAtPattern();
            if(!endAt.isEmpty()){
                triggerBuilder.endAt(DateUtil.parse(endAt,pattern));
            }

            priority = scheduled.priority();
            if(priority != 0){
                triggerBuilder.withPriority(priority);
            }

            JobDataMap jobData = new JobDataMap();
            triggerBuilder.usingJobData(jobData);

            trigger = triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(scheduled.cron())).build();
            try {
                scheduler = schedulerFactory.getScheduler(scheduled.name() + index);
                job = new DefaultJob(m);
                jobDetail = JobBuilder.newJob(job.getClass()).withIdentity("JOB_" + index,
                        "JOB_GROUP_" + index).setJobData(jobData).build();
                delay = scheduled.delay();
                if(delay != 0){
                    scheduler.startDelayed(delay);
                }
                scheduler.scheduleJob(jobDetail,trigger);
                scheduler.start();
                //todo shutdown now
            } catch (SchedulerException e) {
                e.printStackTrace();
                throw new ExceptionWrapper(e);
            }
        }
    }

}
