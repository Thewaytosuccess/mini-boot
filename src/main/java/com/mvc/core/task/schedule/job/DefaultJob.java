package com.mvc.core.task.schedule.job;

import com.mvc.core.injection.IocContainer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.lang.reflect.Method;

/**
 * @author xhzy
 */
public class DefaultJob implements Job {

    private final Method method;

    public DefaultJob(Method method){
        this.method = method;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext){
        try {
            System.out.println("hello world");
            method.invoke(IocContainer.getInstance().getClassInstance(method.getDeclaringClass()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
