package com.mvc.core.injection;

import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.type.controller.RestController;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * 启动类：程序入口
     */
    private Class<?> springBootApplication;

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
        return Objects.nonNull(classes) ? classes : new ArrayList<>();
    }

    public Object getClassInstance(Class<?> clazz){
        return Objects.nonNull(iocContainer) ? iocContainer.get(clazz) : null;
    }

    public Class<?> getSpringBootApplication(){
        if(classes.size() == 1){
            return classes.get(0);
        }else{
            if(classes.get(classes.size() - 1) != SpringBootApplication.class){
                return classes.get(classes.size() - 2);
            }else{
                return classes.get(classes.size() - 1);
            }
        }
    }

    public List<Class<?>> getControllers() {
        Optional<List<Class<?>>> classes = Optional.ofNullable(this.classes);
        return classes.map(classList -> classList.stream().filter(c -> c.isAnnotationPresent(RestController.class)
                || c.isAnnotationPresent(Controller.class)).collect(Collectors.toList())).orElse(Collections.emptyList());
    }
}
