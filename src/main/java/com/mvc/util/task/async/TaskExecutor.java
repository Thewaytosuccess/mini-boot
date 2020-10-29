package com.mvc.util.task.async;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author xhzy
 */
public class TaskExecutor {

    private static final TaskExecutor TASK_EXECUTOR = new TaskExecutor();

    private TaskExecutor(){}

    public static TaskExecutor getInstance(){ return TASK_EXECUTOR; }

    public ThreadPoolExecutor executor;

    private static final String THREAD_NAME_PREFIX = "default_thread_pool";

    @PostConstruct
    public void initPool(){
        executor = new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), new ThreadFactoryImpl(THREAD_NAME_PREFIX));
    }

    public ThreadPoolExecutor getExecutor(){
        if(Objects.isNull(executor)){
            initPool();
        }
        return executor;
    }

    @PreDestroy
    public void shutdown(){
        if(Objects.nonNull(executor)){
            executor.shutdown();
        }
    }


}
