package com.boot.mini.entity.test;

import java.io.Serializable;

/**
 * @author xhzy
 */
public class ScheduleParam implements Serializable {

    private Integer jobId;

    private String cron;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
