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
        }else{
            return;
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
        //用于编号
        AtomicInteger count = new AtomicInteger();
        for (Method method:tasks){
            try {
                buildScheduler(method,schedulerFactory,count).start();
                //todo shutdown now
            } catch (SchedulerException e) {
                e.printStackTrace();
                throw new ExceptionWrapper(e);
            }
        }
    }

    private Scheduler buildScheduler(Method method,StdSchedulerFactory schedulerFactory,
                                     AtomicInteger count) throws SchedulerException{
        int index = count.incrementAndGet();
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        Class<? extends ScheduleConfigAdapter> config = scheduled.config();
        if(config != ScheduleConfigAdapter.class){

        }
        String scheduleName = scheduled.name();
        if(scheduleName.isEmpty()){
            scheduleName = "DEFAULT_SCHEDULE_" + index;
        }

        Scheduler scheduler = schedulerFactory.getScheduler(scheduleName);
        int delay = scheduled.delay();
        if(delay != 0){
            scheduler.startDelayed(delay);
        }
        scheduler.scheduleJob(buildJob(scheduled,index,method),buildTrigger(scheduled,index));
        return scheduler;
    }

    private Trigger buildTrigger(Scheduled scheduled,int index){
        String triggerName = scheduled.triggerName();
        if(triggerName.isEmpty()){
            triggerName = "TRIGGER_" + index;
        }
        String triggerGroup = scheduled.triggerGroup();
        if(triggerGroup.isEmpty()){
            triggerGroup = "TRIGGER_GROUP_" + index;
        }
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(triggerName,triggerGroup);

        String startAt = scheduled.startAt();
        String pattern = scheduled.startAtPattern();
        if(!startAt.isEmpty()){
            triggerBuilder.startAt(DateUtil.parse(startAt,pattern));
        }else{
            triggerBuilder.startNow();
        }

        String endAt = scheduled.endAt();
        pattern = scheduled.endAtPattern();
        if(!endAt.isEmpty()){
            triggerBuilder.endAt(DateUtil.parse(endAt,pattern));
        }

        int priority = scheduled.priority();
        if(priority != 0){
            triggerBuilder.withPriority(priority);
        }

        JobDataMap jobData = new JobDataMap();
        triggerBuilder.usingJobData(jobData);
        return  triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(scheduled.cron())).build();
    }

    private JobDetail buildJob(Scheduled scheduled,int index,Method method){
        DefaultJob job = new DefaultJob(method);
        String jobName = scheduled.jobName();
        if(jobName.isEmpty()){
            jobName = "JOB_" + index;
        }

        String jobGroup = scheduled.jobGroup();
        if(jobGroup.isEmpty()){
            jobGroup = "JOB_GROUP_" + index;
        }
        return JobBuilder.newJob(job.getClass()).withIdentity(jobName, jobGroup).
                setJobData(new JobDataMap()).build();
    }

}
