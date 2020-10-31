package com.mvc.core.task.schedule.config;

import org.quartz.JobDataMap;

/**
 * @author xhzy
 */
public interface ScheduleConfigAdapter {

    /**
     * 定时任务配置
     * @return 配置参数
     */
    ScheduleConfig setConfig();

    /**
     * 运行时传参
     * @return 参数
     */
    JobDataMap setData();
}
