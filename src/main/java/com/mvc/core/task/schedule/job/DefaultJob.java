package com.mvc.core.task.schedule.job;

import com.mvc.core.injection.IocContainer;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.lang.reflect.Method;

/**
 * @author xhzy
 */
public class DefaultJob implements Job {

    @Override
    public void execute(JobExecutionContext context){
        try {
            Object task = context.getJobDetail().getJobDataMap().get("method");
            if(task instanceof Method){
                Method method = (Method)task;
                method.invoke(IocContainer.getInstance().getClassInstance(method.getDeclaringClass()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
