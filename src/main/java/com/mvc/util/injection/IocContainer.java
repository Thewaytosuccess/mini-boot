package com.mvc.util.injection;

import com.mvc.annotation.aop.aspect.Interceptor;
import com.mvc.annotation.bean.life.PostConstruct;
import com.mvc.annotation.bean.life.PreDestroy;
import com.mvc.annotation.config.Configuration;
import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.annotation.type.component.Component;
import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.annotation.type.service.Service;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.util.aspect.AspectProcessor;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.interceptor.HandlerInterceptor;
import com.mvc.util.interceptor.InterceptorProcessor;

import java.lang.reflect.Method;
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

    private Set<String> preDestroySet;

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
            return Collections.emptyList();
        }
        return classes;
    }

    public Object getClassInstance(Class<?> clazz){
        if(Objects.isNull(iocContainer)){
            return null;
        }
        return iocContainer.get(clazz);
    }

    /**
     * 解析@RequestMapping,@GetMapping,@PostMapping所注解的方法，拼接成url,
     * 统一注册到map<url,package.class.method>
     */
    public void inject(){
        if(Objects.isNull(classes)){
            return;
        }
        classes.forEach(clazz -> {
            try {
                //待处理@Controller
                if(clazz.isAnnotationPresent(Configuration.class) || clazz.isAnnotationPresent(Component.class) ||
                   clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(ControllerAdvice.class) ||
                   clazz.isAnnotationPresent(Interceptor.class) || clazz.isAnnotationPresent(RestController.class)
                   || clazz.isAnnotationPresent(Controller.class)){
                    //ioc
                    DependencyInjectProcessor.getInstance().inject(clazz);
                }
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        });
    }

    public void reInject(){
        if(AspectProcessor.getInstance().reInjected()){
            Set<Class<?>> classes = new HashSet<>();
            Map<String, Class<?>[]> reInjected = AspectProcessor.getInstance().getReInjected();
            reInjected.forEach((k,v) -> {
                try {
                    classes.add(Class.forName(k));
                    if(v.length > 0){
                        classes.addAll(Arrays.asList(v));
                    }
                } catch (ClassNotFoundException e) {
                    throw new ExceptionWrapper(e);
                }
            });
            if(!classes.isEmpty()){
                //将代理对象重新注入到依赖它的类中
                getClasses().forEach(e -> reInject(e,classes));
            }
        }
    }

    private void reInject(Class<?> clazz, Set<Class<?>> classes){
        try {
            DependencyInjectProcessor.getInstance().reInject(clazz,classes);
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    public void aspectScan() {
        if(Objects.isNull(classes)){
            return;
        }
        classes.forEach(clazz -> {
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
        });
    }

    public void init(){
        if(Objects.isNull(classes)){
            return;
        }
        classes.forEach(clazz -> {
            try {
                Method[] declaredMethods = clazz.getDeclaredMethods();
                for(Method m:declaredMethods){
                    if(m.isAnnotationPresent(PostConstruct.class)){
                        m.invoke(IocContainer.getInstance().getClassInstance(clazz));
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

    public void destroy(){
        if(Objects.isNull(preDestroySet)){
            return;
        }
        preDestroySet.forEach(k -> {
            int index = k.lastIndexOf(ConstantPool.PATH_SEPARATOR);
            try {
                Class<?> clazz = Class.forName(k.substring(0, index));
                clazz.getDeclaredMethod(k.substring(index + 1)).invoke(
                        IocContainer.getInstance().getClassInstance(clazz));
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        });
    }

    public List<Class<?>> getControllers() {
        if(Objects.isNull(classes)){
            return Collections.emptyList();
        }
        return classes.stream().filter(e -> e.isAnnotationPresent(RestController.class) ||
                e.isAnnotationPresent(Controller.class)).collect(Collectors.toList());
    }
}
