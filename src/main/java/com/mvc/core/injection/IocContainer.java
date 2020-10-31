package com.mvc.core.injection;

import com.mvc.annotation.aop.aspect.Interceptor;
import com.mvc.annotation.config.Configuration;
import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.annotation.type.component.Component;
import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.annotation.type.service.Service;
import com.mvc.core.aspect.AspectProcessor;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.interceptor.HandlerInterceptor;
import com.mvc.core.interceptor.InterceptorProcessor;

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

    /**
     * 解析@RequestMapping,@GetMapping,@PostMapping所注解的方法，拼接成url,
     * 统一注册到map<url,package.class.method>
     */
    public void inject(){
        Optional.ofNullable(classes).ifPresent(e ->
            e.forEach(clazz -> {
                try {
                    //待处理@Controller
                    if(clazz.isAnnotationPresent(Configuration.class) || clazz.isAnnotationPresent(Component.class) ||
                       clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(ControllerAdvice.class) ||
                       clazz.isAnnotationPresent(Interceptor.class) || clazz.isAnnotationPresent(RestController.class)
                       || clazz.isAnnotationPresent(Controller.class)){
                        //ioc
                        DependencyInjectProcessor.getInstance().inject(clazz);
                    }
                } catch (Exception ex) {
                    throw new ExceptionWrapper(ex);
                }
            })
        );

    }

    public void reInject(){
        if(AspectProcessor.getInstance().reInjected()){
            Set<Class<?>> set = new HashSet<>();
            Map<Class<?>, Class<?>[]> reInjected = AspectProcessor.getInstance().getReInjected();
            reInjected.forEach((k,v) -> {
                set.add(k);
                set.addAll(Arrays.asList(v));
            });
            if(!set.isEmpty()){
                //将代理对象重新注入到依赖它的类中
                DependencyInjectProcessor injectProcessor = DependencyInjectProcessor.getInstance();
                this.classes.forEach(e -> injectProcessor.reInject(e,set));
            }
        }
    }

    public void aspectScan() {
        Optional.ofNullable(classes).ifPresent(e ->
            e.forEach(clazz -> {
                if(clazz.isAnnotationPresent(Configuration.class) || clazz.isAnnotationPresent(Component.class)){
                    //aop,register aspect
                    AspectProcessor.getInstance().process(clazz);
                    if(Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)){
                        //register interceptor
                        InterceptorProcessor.getInstance().add(clazz);
                    }
                }else if(clazz.isAnnotationPresent(Interceptor.class)){
                    if(Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)){
                        //register interceptor
                        InterceptorProcessor.getInstance().add(clazz);
                    }
                }
            })
        );
    }

    public List<Class<?>> getControllers() {
        Optional<List<Class<?>>> classes = Optional.ofNullable(this.classes);
        return classes.map(classList -> classList.stream().filter(c -> c.isAnnotationPresent(RestController.class)
                || c.isAnnotationPresent(Controller.class)).collect(Collectors.toList())).orElse(Collections.emptyList());
    }
}
