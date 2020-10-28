package com.mvc.util.async;

import com.mvc.annotation.bean.life.PostConstruct;
import com.mvc.annotation.bean.life.PreDestroy;
import com.mvc.annotation.type.component.Component;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xhzy
 */
@Component
public class TaskExecutor {

    public ThreadPoolExecutor executor;

    private static final String THREAD_NAME_PREFIX = "default_thread_pool";

    @PostConstruct
    public void initPool(){
        executor = new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), new ThreadFactoryImpl(THREAD_NAME_PREFIX));
    }

    public void submit(Runnable runnable){
        executor.submit(() -> runnable);
    }

    @PreDestroy
    public void shutdown(){
        if(Objects.nonNull(executor)){
            executor.shutdown();
        }
    }


}
