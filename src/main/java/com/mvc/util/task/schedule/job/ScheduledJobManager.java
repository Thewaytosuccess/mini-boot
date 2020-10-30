package com.mvc.util.task.schedule.job;

import com.mvc.annotation.enable.EnableScheduling;
import com.mvc.annotation.method.schedule.Scheduled;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.util.DateUtil;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.injection.IocContainer;
import com.mvc.util.task.schedule.config.DefaultScheduleConfig;
import com.mvc.util.task.schedule.config.ScheduleConfig;
import com.mvc.util.task.schedule.config.ScheduleConfigAdapter;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class ScheduledJobManager {

    private static final ScheduledJobManager MANAGER = new ScheduledJobManager();

    private ScheduledJobManager(){}

    public static ScheduledJobManager getInstance(){ return MANAGER; }

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
        //用于编号
        AtomicInteger count = new AtomicInteger();
        for (Method method:tasks){
            try {
                buildScheduler(method,schedulerFactory,count).start();
                //todo shutdown by config
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExceptionWrapper(e);
            }
        }
    }

    private ScheduleConfig getScheduleConfig(Method method) throws Exception{
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        Class<? extends ScheduleConfigAdapter> clazz = scheduled.configClass();

        ScheduleConfig config;
        if(clazz != DefaultScheduleConfig.class){
            Object instance = clazz.newInstance();
            config = (ScheduleConfig)clazz.getDeclaredMethod("setConfig").invoke(instance);
            JobDataMap jobData = (JobDataMap)clazz.getDeclaredMethod("setData").invoke(instance);
            if(Objects.nonNull(jobData)){
                config.setJobDataMap(jobData);
            }
        }else{
            config = new ScheduleConfig();
            config.setCron(scheduled.cron());
            config.setName(scheduled.name());
            config.setDelay(scheduled.delay());
            config.setPriority(scheduled.priority());
            config.setStartAt(scheduled.startAt());
            config.setEndAt(scheduled.endAt());
            config.setStartAtPattern(scheduled.startAtPattern());
            config.setEndAtPattern(scheduled.endAtPattern());
            config.setJobName(scheduled.jobName());
            config.setJobGroup(scheduled.jobGroup());
            config.setTriggerName(scheduled.triggerName());
            config.setTriggerGroup(scheduled.triggerGroup());
        }
        return config;
    }

    private Scheduler buildScheduler(Method method,StdSchedulerFactory schedulerFactory,
                                     AtomicInteger count) throws Exception{
        int index = count.incrementAndGet();
        ScheduleConfig config = getScheduleConfig(method);
        String scheduleName = config.getName();
        if(scheduleName.isEmpty()){
            scheduleName = "DEFAULT_SCHEDULE_" + index;
        }

        Scheduler scheduler = schedulerFactory.getScheduler(scheduleName);
        int delay = config.getDelay();
        if(delay != 0){
            scheduler.startDelayed(delay);
        }
        scheduler.scheduleJob(buildJob(config,index,method),buildTrigger(config,index));
        return scheduler;
    }

    private Trigger buildTrigger(ScheduleConfig config,int index){
        String triggerName = config.getTriggerName();
        if(triggerName.isEmpty()){
            triggerName = "TRIGGER_" + index;
        }
        String triggerGroup = config.getTriggerGroup();
        if(triggerGroup.isEmpty()){
            triggerGroup = "TRIGGER_GROUP_" + index;
        }
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(triggerName,triggerGroup);

        String startAt = config.getStartAt();
        String pattern = config.getStartAtPattern();
        if(!startAt.isEmpty()){
            triggerBuilder.startAt(DateUtil.parse(startAt,pattern));
        }else{
            triggerBuilder.startNow();
        }

        String endAt = config.getEndAt();
        pattern = config.getEndAtPattern();
        if(!endAt.isEmpty()){
            triggerBuilder.endAt(DateUtil.parse(endAt,pattern));
        }

        int priority = config.getPriority();
        if(priority != 0){
            triggerBuilder.withPriority(priority);
        }

        JobDataMap jobData = config.getJobDataMap();
        if(Objects.nonNull(jobData)){
            triggerBuilder.usingJobData(jobData);
        }
        return triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(config.getCron())).build();
    }

    private JobDetail buildJob(ScheduleConfig config,int index,Method method){
        DefaultJob job = new DefaultJob(method);
        String jobName = config.getJobName();
        if(jobName.isEmpty()){
            jobName = "JOB_" + index;
        }

        String jobGroup = config.getJobGroup();
        if(jobGroup.isEmpty()){
            jobGroup = "JOB_GROUP_" + index;
        }

        JobBuilder jobBuilder = JobBuilder.newJob(job.getClass()).withIdentity(jobName, jobGroup);
        JobDataMap jobData = config.getJobDataMap();
        if(Objects.nonNull(jobData)){
            jobBuilder.setJobData(jobData);
        }
        return jobBuilder.build();
    }

}
