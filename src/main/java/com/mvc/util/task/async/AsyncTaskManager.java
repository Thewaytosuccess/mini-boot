package com.mvc.util.task.async;

import com.mvc.annotation.method.async.Async;
import com.mvc.annotation.enable.EnableAsync;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.entity.method.Signature;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.util.injection.IocContainer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class AsyncTaskManager {

    private static final AsyncTaskManager MANAGER = new AsyncTaskManager();

    private AsyncTaskManager(){}

    public static AsyncTaskManager getInstance(){
        return MANAGER;
    }

    private Set<Signature> tasks;

    public boolean isAsync(Signature signature){
        if(Objects.isNull(tasks)){
            return false;
        }
        return tasks.contains(signature);
    }

    public Set<Signature> scan(){
        if(Objects.isNull(tasks)){
            tasks = new HashSet<>();
        }

        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        boolean global = classes.stream().anyMatch(e -> e.isAnnotationPresent(SpringBootApplication.class) &&
                e.isAnnotationPresent(EnableAsync.class));
        if(!global){
            classes = classes.stream().filter(e -> e.isAnnotationPresent(EnableAsync.class)).collect(Collectors.toList());
        }
        classes.forEach(e -> tasks.addAll(Arrays.stream(e.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Async.class))
                .map(this::getSignature).collect(Collectors.toSet())));
        return tasks;
    }

    private Signature getSignature(Method method) {
        return new Signature(method.getParameterCount(),method.getParameterTypes(),
                method.getDeclaringClass().getName() + ConstantPool.PATH_SEPARATOR + method.getName());
    }
}
