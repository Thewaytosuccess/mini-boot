package com.boot.mini.core.task.async;

import com.boot.mini.annotation.method.async.Async;
import com.boot.mini.core.injection.IocContainer;
import com.boot.mini.core.mapping.PackageScanner;
import com.boot.mini.entity.method.Signature;
import com.boot.mini.annotation.enable.EnableAsync;
import com.boot.mini.enums.constant.ConstantPool;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
        return Objects.nonNull(tasks) && tasks.contains(signature);
    }

    public Set<Signature> scan(){
        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        AtomicBoolean global = new AtomicBoolean(false);
        Optional.ofNullable(PackageScanner.getInstance().getStarterClass()).ifPresent(e ->
                global.set(e.isAnnotationPresent(EnableAsync.class)));
        if(!global.get()){
            classes = classes.stream().filter(e -> e.isAnnotationPresent(EnableAsync.class)).collect(Collectors.toList());
        }

        if(!classes.isEmpty()){
            tasks = new HashSet<>();
        }
        classes.forEach(e -> tasks.addAll(Arrays.stream(e.getDeclaredMethods()).filter(m ->
                m.isAnnotationPresent(Async.class)).map(this::getSignature).collect(Collectors.toSet())));
        return tasks;
    }

    private Signature getSignature(Method method) {
        return new Signature(method.getParameterCount(),method.getParameterTypes(),
                method.getDeclaringClass().getName() + ConstantPool.PATH_SEPARATOR + method.getName());
    }
}
