package com.mvc.core.task.init;

import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.IocContainer;
import com.mvc.core.task.async.TaskExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author xhzy
 */
public class BeanInitializer {

    private static final BeanInitializer MANAGER = new BeanInitializer();

    private BeanInitializer(){}

    public static BeanInitializer getInstance(){ return MANAGER; }

    private Set<Method> preDestroySet;

    /**
     * bean初始化
     */
    public void init(){
        Optional.ofNullable(IocContainer.getInstance().getClasses()).ifPresent(e -> {
            ThreadPoolExecutor executor = TaskExecutor.getInstance().getExecutor();
            e.forEach(clazz -> {
                try {
                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.isAnnotationPresent(PostConstruct.class)) {
                            executor.submit(() -> m.invoke(IocContainer.getInstance().getClassInstance(clazz)));
                        } else if (m.isAnnotationPresent(PreDestroy.class)) {
                            preDestroySet = getPreDestroySet(preDestroySet);
                            preDestroySet.add(m);
                        }
                    }
                } catch (Exception ex) {
                    throw new ExceptionWrapper(ex);
                }
            });
        });
    }

    private Set<Method> getPreDestroySet(Set<Method> preDestroySet){
        return Objects.isNull(preDestroySet) ? new HashSet<>() : preDestroySet;
    }

    /**
     * bean销毁
     */
    public void destroy(){
        Optional.ofNullable(preDestroySet).ifPresent(methods -> {
            ThreadPoolExecutor executor = TaskExecutor.getInstance().getExecutor();
            methods.forEach(m -> {
                try {
                    executor.submit(() -> m.invoke(IocContainer.getInstance().getClassInstance(m.getDeclaringClass())));
                } catch (Exception ex) {
                    throw new ExceptionWrapper(ex);
                }
            });
        });
    }
}
