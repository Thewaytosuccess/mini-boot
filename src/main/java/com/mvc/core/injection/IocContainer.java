package com.mvc.core.injection;

import java.util.*;

/**
 * @author xhzy
 */
public class IocContainer {

    private static final IocContainer CONTAINER = new IocContainer();

    private IocContainer(){}

    public static IocContainer getInstance(){
        return CONTAINER;
    }

    /**
     * IOC容器中所有元素的KEY
     */
    private List<Class<?>> classes;

    /**
     * IOC容器：装载类和对应的实例
     */
    private Map<Class<?>,Object> iocContainer;

    public void addClass(Class<?> clazz){
        if(Objects.isNull(clazz)){
            classes = new ArrayList<>();
        }
        classes.add(clazz);
    }

    public void addInstance(Class<?> clazz,Object instance){
        if(Objects.isNull(iocContainer)){
            iocContainer = new HashMap<>(16);
        }
        iocContainer.put(clazz,instance);
    }

    public List<Class<?>> getClasses(){
        if(Objects.isNull(classes)){
            classes = new ArrayList<>();
        }
        return classes;
    }

    public Object getClassInstance(Class<?> clazz){
        return Objects.nonNull(iocContainer) ? iocContainer.get(clazz) : null;
    }
}
