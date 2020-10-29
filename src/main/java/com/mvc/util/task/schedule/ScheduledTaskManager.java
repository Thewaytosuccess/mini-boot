package com.mvc.util.task.schedule;

import com.mvc.annotation.enable.EnableScheduling;
import com.mvc.annotation.method.schedule.Scheduled;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.util.injection.IocContainer;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.lang.reflect.Method;
import java.util.*;
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
        }

        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        boolean global = classes.stream().anyMatch(e -> e.isAnnotationPresent(SpringBootApplication.class) &&
                e.isAnnotationPresent(EnableScheduling.class));
        if(!global){
            classes = classes.stream().filter(e -> e.isAnnotationPresent(EnableScheduling.class)).collect(Collectors.toList());
        }
        classes.forEach(e -> tasks.addAll(Arrays.stream(e.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Scheduled.class))
                .collect(Collectors.toSet())));
        //todo 使用quartz/xxl-job/elastic-job执行定时任务

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity("name", "group");
        for (Method m:tasks){
            Scheduled scheduled = m.getAnnotation(Scheduled.class);
            String startAt = scheduled.startAt();
            if(!startAt.isEmpty()){
               // triggerBuilder.startAt()
            }
        }
    }

}
