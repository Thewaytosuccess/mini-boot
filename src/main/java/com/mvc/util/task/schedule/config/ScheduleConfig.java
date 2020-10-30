package com.mvc.util.task.schedule.config;

import com.sun.istack.internal.NotNull;
import org.quartz.JobDataMap;

/**
 * @author xhzy
 */
public class ScheduleConfig {

    @NotNull
    private String cron;

    private String name;

    private int delay;

    private int priority;

    private String startAt;

    private String endAt;

    private String startAtPattern;

    private String endAtPattern;

    private String jobName;

    private String jobGroup;

    private String triggerName;

    private String triggerGroup;

    private String prefix;

    private JobDataMap jobDataMap;

    public void setJobDataMap(JobDataMap jobDataMap) {
        this.jobDataMap = jobDataMap;
    }

    public JobDataMap getJobDataMap() {
        return jobDataMap;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public String getStartAtPattern() {
        return startAtPattern;
    }

    public void setStartAtPattern(String startAtPattern) {
        this.startAtPattern = startAtPattern;
    }

    public String getEndAtPattern() {
        return endAtPattern;
    }

    public void setEndAtPattern(String endAtPattern) {
        this.endAtPattern = endAtPattern;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }
}
