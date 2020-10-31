package com.mvc.core.task.schedule.job;

import com.mvc.annotation.enable.EnableScheduling;
import com.mvc.annotation.method.schedule.Scheduled;
import com.mvc.enums.ExceptionEnum;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.core.util.DateUtil;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.ConfigurationProcessor;
import com.mvc.core.injection.IocContainer;
import com.mvc.core.task.schedule.config.DefaultScheduleConfig;
import com.mvc.core.task.schedule.config.ScheduleConfig;
import com.mvc.core.task.schedule.config.ScheduleConfigAdapter;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public void init(){
        IocContainer container = IocContainer.getInstance();
        AtomicBoolean global = new AtomicBoolean(false);
        Optional.ofNullable(container.getSpringBootApplication()).ifPresent(e ->
                global.set(e.isAnnotationPresent(EnableScheduling.class)));

        List<Class<?>> classes = container.getClasses();
        if(!global.get()){
            classes = classes.stream().filter(e -> e.isAnnotationPresent(EnableScheduling.class))
                    .collect(Collectors.toList());
        }

        if(!classes.isEmpty()){
            tasks = new HashSet<>();
        }
        classes.forEach(e -> tasks.addAll(Arrays.stream(e.getDeclaredMethods()).filter(m ->
                m.isAnnotationPresent(Scheduled.class)).collect(Collectors.toSet())));
        if(Objects.nonNull(tasks) && !tasks.isEmpty()){
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
            config = DefaultScheduleConfig.getConfig(scheduled.prefix());
            if(Objects.isNull(config)){
                config = new ScheduleConfig();
                config.setCron(getValue(scheduled.cron()));
                config.setName(getValue(scheduled.name()));
                config.setDelay(getValue(scheduled.delay()));
                config.setPriority(getValue(scheduled.priority()));
                config.setStartAt(getValue(scheduled.startAt()));
                config.setEndAt(getValue(scheduled.endAt()));
                config.setStartAtPattern(getValue(scheduled.startAtPattern()));
                config.setEndAtPattern(getValue(scheduled.endAtPattern()));
                config.setJobName(getValue(scheduled.jobName()));
                config.setJobGroup(getValue(scheduled.jobGroup()));
                config.setTriggerName(getValue(scheduled.triggerName()));
                config.setTriggerGroup(getValue(scheduled.triggerGroup()));
            }
        }

        String cron = config.getCron();
        if(Objects.isNull(cron) || cron.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
        return config;
    }

    private String getValue(String key){
        if(key.startsWith(ConstantPool.KEY_PREFIX) && key.endsWith(ConstantPool.KEY_SUFFIX)){
            key = key.substring(2, key.length() - 1);
            if(!key.isEmpty()){
                return ConfigurationProcessor.getInstance().get(key);
            }else{
                throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
            }
        }
        return key;
    }

    private Scheduler buildScheduler(Method method,StdSchedulerFactory schedulerFactory, AtomicInteger count)
            throws Exception{
        int index = count.incrementAndGet();
        ScheduleConfig config = getScheduleConfig(method);
        String scheduleName = config.getName();
        if(Objects.isNull(scheduleName) || scheduleName.isEmpty()){
            scheduleName = "DEFAULT_SCHEDULE_" + index;
        }

        Scheduler scheduler = schedulerFactory.getScheduler(scheduleName);
        String delay = config.getDelay();
        if(Objects.nonNull(delay) && !delay.isEmpty()){
            scheduler.startDelayed(Integer.parseInt(delay));
        }
        scheduler.scheduleJob(buildJob(config,index,method),buildTrigger(config,index));
        return scheduler;
    }

    private Trigger buildTrigger(ScheduleConfig config,int index){
        String triggerName = config.getTriggerName();
        if(Objects.isNull(triggerName) || triggerName.isEmpty()){
            triggerName = "TRIGGER_" + index;
        }
        String triggerGroup = config.getTriggerGroup();
        if(Objects.isNull(triggerGroup) || triggerGroup.isEmpty()){
            triggerGroup = "TRIGGER_GROUP_" + index;
        }
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(triggerName,triggerGroup);

        String startAt = config.getStartAt();
        String pattern = config.getStartAtPattern();
        if(Objects.nonNull(startAt) && !startAt.isEmpty()){
            triggerBuilder.startAt(DateUtil.parse(startAt,pattern));
        }else{
            triggerBuilder.startNow();
        }

        String endAt = config.getEndAt();
        pattern = config.getEndAtPattern();
        if(Objects.nonNull(endAt) && !endAt.isEmpty()){
            triggerBuilder.endAt(DateUtil.parse(endAt,pattern));
        }

        String priority = config.getPriority();
        if(Objects.nonNull(priority) && !priority.isEmpty()){
            triggerBuilder.withPriority(Integer.parseInt(priority));
        }

        JobDataMap jobData = config.getJobDataMap();
        if(Objects.nonNull(jobData) && !jobData.isEmpty()){
            triggerBuilder.usingJobData(jobData);
        }
        return triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(config.getCron())).build();
    }

    private JobDetail buildJob(ScheduleConfig config,int index,Method method){
        DefaultJob job = new DefaultJob(method);
        String jobName = config.getJobName();
        if(Objects.isNull(jobName) || jobName.isEmpty()){
            jobName = "JOB_" + index;
        }

        String jobGroup = config.getJobGroup();
        if(Objects.isNull(jobGroup) || jobGroup.isEmpty()){
            jobGroup = "JOB_GROUP_" + index;
        }

        JobBuilder jobBuilder = JobBuilder.newJob(job.getClass()).withIdentity(jobName, jobGroup);
        JobDataMap jobData = config.getJobDataMap();
        if(Objects.nonNull(jobData) && !jobData.isEmpty()){
            jobBuilder.setJobData(jobData);
        }
        return jobBuilder.build();
    }

}
