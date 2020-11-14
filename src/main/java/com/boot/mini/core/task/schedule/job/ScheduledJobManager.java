package com.boot.mini.core.task.schedule.job;

import com.alibaba.fastjson.JSON;
import com.boot.mini.annotation.method.schedule.Scheduled;
import com.boot.mini.core.injection.ConfigurationProcessor;
import com.boot.mini.core.injection.IocContainer;
import com.boot.mini.core.mapping.PackageScanner;
import com.boot.mini.core.task.async.TaskExecutor;
import com.boot.mini.core.task.schedule.config.DefaultScheduleConfig;
import com.boot.mini.core.task.schedule.config.ScheduleConfig;
import com.boot.mini.core.task.schedule.config.ScheduleConfigAdapter;
import com.boot.mini.core.util.DateUtil;
import com.boot.mini.enums.constant.ConstantPool;
import com.boot.mini.annotation.enable.EnableScheduling;
import com.boot.mini.enums.ExceptionEnum;
import com.boot.mini.core.exception.ExceptionWrapper;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class ScheduledJobManager {

    private static final ScheduledJobManager MANAGER = new ScheduledJobManager();

    private ScheduledJobManager(){}

    public static ScheduledJobManager getInstance(){ return MANAGER; }

    private List<Method> tasks;

    Scheduler scheduler;

    private Map<Integer,JobKey> jobKeyMap;

    private Map<Integer,TriggerKey> triggerKeyMap;

    @PostConstruct
    public void init(){
        AtomicBoolean global = new AtomicBoolean(false);
        Optional.ofNullable(PackageScanner.getInstance().getStarterClass()).ifPresent(e ->
                global.set(e.isAnnotationPresent(EnableScheduling.class)));

        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        if(!global.get()){
            classes = classes.stream().filter(e -> e.isAnnotationPresent(EnableScheduling.class)).collect(Collectors.toList());
        }

        if(!classes.isEmpty()){
            tasks = new ArrayList<>();
        }
        classes.forEach(e -> tasks.addAll(Arrays.stream(e.getDeclaredMethods()).filter(m ->
                m.isAnnotationPresent(Scheduled.class)).collect(Collectors.toSet())));
        if(Objects.nonNull(tasks) && !tasks.isEmpty()){
            initJob();
        }
    }

    @PreDestroy
    public void destroy(){
        ThreadPoolExecutor executor = TaskExecutor.getInstance().getExecutor();
        if(Objects.nonNull(scheduler)){
            executor.submit(() -> {
                try {
                    if(scheduler.isStarted() && !scheduler.isShutdown()){
                        scheduler.shutdown();
                    }
                } catch (SchedulerException ex) {
                    throw new ExceptionWrapper(ex);
                }
            });
        }
    }

    public boolean deleteJob(int jobId){
        try {
            JobKey jobKey = jobKeyMap.get(jobId);
            return Objects.nonNull(jobKey) && scheduler.checkExists(jobKey) &&
                    scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteJobs(List<Integer> jobIds){
        List<JobKey> keys = new ArrayList<>();
        jobKeyMap.forEach((k,v) -> {
            if(jobIds.contains(k)){
                keys.add(v);
            }
        });
        try {
            return scheduler.deleteJobs(keys);
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void pauseJob(int jobId){
        try {
            JobKey jobKey = jobKeyMap.get(jobId);
            if(Objects.nonNull(jobKey) && scheduler.checkExists(jobKey)){
                scheduler.pauseJob(jobKey);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void resumeJob(int jobId){
        try {
            JobKey jobKey = jobKeyMap.get(jobId);
            if(Objects.nonNull(jobKey) && scheduler.checkExists(jobKey)){
                scheduler.resumeJob(jobKey);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void startJob(int jobId){
        try {
            JobKey jobKey = jobKeyMap.get(jobId);
            if(Objects.nonNull(jobKey) && scheduler.checkExists(jobKey)){
                scheduler.triggerJob(jobKey);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void rescheduleJob(int jobId,String cron){
        if(Objects.isNull(cron) || cron.isEmpty()){
            return;
        }

        try {
            TriggerKey triggerKey = triggerKeyMap.get(jobId);
            if(Objects.nonNull(triggerKey) && scheduler.checkExists(jobKeyMap.get(jobId))){
                Trigger trigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder
                        .cronSchedule(cron)).withIdentity(triggerKey.getName(),triggerKey.getGroup()).build();
                scheduler.rescheduleJob(triggerKey,trigger);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * todo 使用xxl-job/elastic-job执行定时任务
     */
    private void initJob(){
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            //用于编号
            for (int i = 0,size = tasks.size(); i < size; ++i) {
                buildScheduler(tasks.get(i), scheduler, i);
                scheduler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        System.out.println("[schedule config] = "+JSON.toJSONString(config));
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

    private void buildScheduler(Method method,Scheduler scheduler, int index) throws Exception{
        ScheduleConfig config = getScheduleConfig(method);
        String delay = config.getDelay();
        if(Objects.nonNull(delay) && !delay.isEmpty()){
            scheduler.startDelayed(Integer.parseInt(delay));
        }

        JobDataMap jobDataMap = config.getJobDataMap();
        if(Objects.isNull(jobDataMap)){
            jobDataMap = new JobDataMap();
        }
        jobDataMap.put("method",method);
        config.setJobDataMap(jobDataMap);
        scheduler.scheduleJob(buildJob(config,index),buildTrigger(config,index));
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

        if(Objects.isNull(triggerKeyMap)){
            triggerKeyMap = new HashMap<>();
        }
        triggerKeyMap.put(index,TriggerKey.triggerKey(triggerName,triggerGroup));
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

    private JobDetail buildJob(ScheduleConfig config,int index){
        String jobName = config.getJobName();
        if(Objects.isNull(jobName) || jobName.isEmpty()){
            jobName = "JOB_" + index;
        }

        String jobGroup = config.getJobGroup();
        if(Objects.isNull(jobGroup) || jobGroup.isEmpty()){
            jobGroup = "JOB_GROUP_" + index;
        }

        if(Objects.isNull(jobKeyMap)){
            jobKeyMap = new HashMap<>();
        }
        jobKeyMap.put(index,JobKey.jobKey(jobName,jobGroup));

        System.out.println("jobName = "+jobName + ";jobGroup = "+jobGroup);
        JobBuilder jobBuilder = JobBuilder.newJob(DefaultJob.class).withIdentity(jobName, jobGroup);
        JobDataMap jobData = config.getJobDataMap();
        if(Objects.nonNull(jobData) && !jobData.isEmpty()){
            jobBuilder.setJobData(jobData);
        }
        return jobBuilder.build();
    }

}
