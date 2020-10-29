package com.mvc.util.task.life;

import com.mvc.enums.constant.ConstantPool;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.injection.IocContainer;
import com.mvc.util.task.async.TaskExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        if(Objects.isNull(classes)){
            return;
        }
        classes.forEach(clazz -> {
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
        });
    }

    /**
     * bean销毁
     */
    public void destroy(){
        if(Objects.isNull(preDestroySet)){
            return;
        }

        ThreadPoolExecutor executor = TaskExecutor.getInstance().getExecutor();
        preDestroySet.forEach(k -> {
            int index = k.lastIndexOf(ConstantPool.PATH_SEPARATOR);
            try {
                Class<?> clazz = Class.forName(k.substring(0, index));
                executor.submit(() -> clazz.getDeclaredMethod(k.substring(index + 1))
                        .invoke(IocContainer.getInstance().getClassInstance(clazz)));
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        });
    }
}
