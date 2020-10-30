package com.mvc.util.task.schedule;

import com.mvc.util.injection.IocContainer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.lang.reflect.Method;

public class DefaultJob implements Job {

    private final Method method;

    public DefaultJob(Method method){
        this.method = method;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext){
        try {
            method.invoke(IocContainer.getInstance().getClassInstance(method.getDeclaringClass()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
