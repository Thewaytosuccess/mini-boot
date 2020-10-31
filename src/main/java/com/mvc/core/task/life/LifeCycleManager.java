package com.mvc.core.task.life;

import com.mvc.enums.constant.ConstantPool;
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
public class LifeCycleManager {

    private static final LifeCycleManager MANAGER = new LifeCycleManager();

    private LifeCycleManager(){}

    public static LifeCycleManager getInstance(){ return MANAGER; }

    private Set<String> preDestroySet;

    /**
     * bean初始化
     */
    public void init(){
        Optional.ofNullable(IocContainer.getInstance().getClasses()).ifPresent(e ->
            e.forEach(clazz -> {
                try {
                    Method[] declaredMethods = clazz.getDeclaredMethods();
                    ThreadPoolExecutor executor = TaskExecutor.getInstance().getExecutor();
                    for(Method m:declaredMethods){
                        if(m.isAnnotationPresent(PostConstruct.class)){
                            executor.submit(() -> m.invoke(IocContainer.getInstance().getClassInstance(clazz)));
                        }else if(m.isAnnotationPresent(PreDestroy.class)){
                            if(Objects.isNull(preDestroySet)){
                                preDestroySet = new HashSet<>();
                            }
                            preDestroySet.add(clazz.getName() + ConstantPool.PATH_SEPARATOR + m.getName());
                        }
                    }
                } catch (Exception ex) {
                    throw new ExceptionWrapper(ex);
                }
            })
        );
    }

    /**
     * bean销毁
     */
    public void destroy(){
        ThreadPoolExecutor executor = TaskExecutor.getInstance().getExecutor();
        Optional.ofNullable(preDestroySet).ifPresent(e ->
            e.forEach(k -> {
                int index = k.lastIndexOf(ConstantPool.PATH_SEPARATOR);
                try {
                    Class<?> clazz = Class.forName(k.substring(0, index));
                    executor.submit(() -> clazz.getDeclaredMethod(k.substring(index + 1))
                            .invoke(IocContainer.getInstance().getClassInstance(clazz)));
                } catch (Exception ex) {
                    throw new ExceptionWrapper(ex);
                }
            })
        );
    }
}
