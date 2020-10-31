package com.mvc.core.aspect;

import com.mvc.annotation.aop.aspect.Aspect;
import com.mvc.annotation.aop.aspect.Interceptor;
import com.mvc.annotation.config.Configuration;
import com.mvc.annotation.enable.EnableAspectJAutoProxy;
import com.mvc.annotation.type.component.Component;
import com.mvc.core.interceptor.HandlerInterceptor;
import com.mvc.core.interceptor.InterceptorProcessor;
import com.mvc.entity.method.Signature;
import com.mvc.core.injection.IocContainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class AspectHandler {

    private static final AspectHandler HANDLER = new AspectHandler();

    private AspectHandler(){}

    public static AspectHandler getInstance(){ return HANDLER; }

    private List<Class<?>> getClasses(){
        return IocContainer.getInstance().getClasses();
    }

    public void aspectScan() {
        AtomicBoolean global = new AtomicBoolean(false);
        Optional.ofNullable(IocContainer.getInstance().getSpringBootApplication()).ifPresent(e ->
                global.set(e.isAnnotationPresent(EnableAspectJAutoProxy.class)));

        Optional.of(getClasses()).ifPresent(e ->
            e.forEach(clazz -> {
                if(clazz.isAnnotationPresent(Configuration.class) || clazz.isAnnotationPresent(Component.class)){
                    if(!global.get()){
                        //非全局配置
                        if(clazz.isAnnotationPresent(EnableAspectJAutoProxy.class) && clazz.isAnnotationPresent(
                                Aspect.class)){
                            //register aspect
                            AspectProcessor.getInstance().process(clazz);
                        }else if(Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)){
                            //register interceptor
                            InterceptorProcessor.getInstance().add(clazz);
                        }
                    }else{
                        if(clazz.isAnnotationPresent(Aspect.class)){
                            AspectProcessor.getInstance().process(clazz);
                        } else if (Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)) {
                            InterceptorProcessor.getInstance().add(clazz);
                        }
                    }
                } else if (clazz.isAnnotationPresent(Interceptor.class)) {
                    if(Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)){
                        //register interceptor
                        InterceptorProcessor.getInstance().add(clazz);
                    }
                }
            })
        );
    }

    public void createProxy() {
        AspectProcessor aspectProcessor = AspectProcessor.getInstance();
        if(aspectProcessor.rescan()){
            //为携带切面注解的方法生成代理
            Map<Class<?>, Signature> annotationMethodMap = aspectProcessor.getAnnotationMethodMap();
            if(!annotationMethodMap.isEmpty()){
                Set<Class<?>> annotations = annotationMethodMap.keySet();
                List<Signature> methods = new ArrayList<>();
                getClasses().forEach(e -> getAnnotatedMethod(e,annotations,annotationMethodMap, methods));
                aspectProcessor.createProxy(methods);
            }
        }else{
            aspectProcessor.createProxy();
        }
    }

    private void getAnnotatedMethod(Class<?> clazz, Set<Class<?>> annotations, Map<Class<?>, Signature> annotationMethodMap,
                                    List<Signature> methods) {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        Signature signature;
        Annotation[] declaredAnnotations;
        for(Method m:declaredMethods){
            declaredAnnotations = m.getDeclaredAnnotations();
            for(Annotation a:declaredAnnotations){
                if(annotations.contains(a.annotationType())){
                    signature = annotationMethodMap.get(a.annotationType());
                    signature.setMethodName(clazz.getName() + PATH_SEPARATOR + m.getName());
                    methods.add(new Signature(m.getParameterCount(),m.getParameterTypes(), signature.getMethodName(),
                            signature.getAdviceEnum(),signature.getAdviceMethod()));
                    break;
                }
            }
        }
    }

    public void methodScan(Class<?> clazz, Set<Signature> set) {
        Arrays.stream(clazz.getDeclaredMethods()).forEach(e -> set.add(new Signature(e.getParameterCount(),
                e.getParameterTypes(), clazz.getName() + PATH_SEPARATOR + e.getName())));
    }

    public void classScan(String packageName, Set<Signature> set) {
        getClasses().stream().filter(e -> e.getName().startsWith(packageName)).forEach(e -> methodScan(e, set));
    }

    public void patternClassScan(String className, Set<Signature> set) {
        getClasses().stream().filter(e -> e.getName().matches(AspectProcessor.getInstance().toRegExp(className))).
                forEach(e -> methodScan(e,set));
    }

    public void patternMethodScan(String methodName, Set<Signature> set, boolean patternMatch) {
        int index = methodName.lastIndexOf(".");
        String className = methodName.substring(0,index);
        getClasses().stream().filter(e -> e.getName().equals(className)).forEach(e -> {
            Method[] methods = e.getDeclaredMethods();
            for(Method m:methods){
                if(patternMatch){
                    if(m.getName().matches(AspectProcessor.getInstance().toRegExp(methodName.substring(index + 1)))){
                        set.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                                className + PATH_SEPARATOR + m.getName()));
                    }
                }else{
                    if(m.getName().equals(methodName.substring(index + 1))){
                        set.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                                className + PATH_SEPARATOR + m.getName()));
                    }
                }
            }
        });
    }
}