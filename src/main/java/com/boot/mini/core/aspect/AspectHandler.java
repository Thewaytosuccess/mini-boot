package com.boot.mini.core.aspect;

import com.boot.mini.annotation.aop.aspect.Aspect;
import com.boot.mini.annotation.aop.aspect.Interceptor;
import com.boot.mini.annotation.config.Configuration;
import com.boot.mini.annotation.enable.EnableAspectJAutoProxy;
import com.boot.mini.annotation.type.component.Component;
import com.boot.mini.core.interceptor.HandlerInterceptor;
import com.boot.mini.core.interceptor.InterceptorProcessor;
import com.boot.mini.core.mapping.PackageScanner;
import com.boot.mini.entity.method.Signature;
import com.boot.mini.core.injection.IocContainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.boot.mini.enums.constant.ConstantPool.PATH_SEPARATOR;

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
        AtomicBoolean enabled = new AtomicBoolean(false);
        Optional.ofNullable(PackageScanner.getInstance().getStarterClass()).ifPresent(e -> enabled.set(e.isAnnotationPresent(EnableAspectJAutoProxy.class)));
        Optional.of(getClasses()).ifPresent(e ->
            e.forEach(clazz -> {
                if(clazz.isAnnotationPresent(Configuration.class) || clazz.isAnnotationPresent(Component.class)){
                    if(!enabled.get()){
                        //非全局配置
                        if(clazz.isAnnotationPresent(EnableAspectJAutoProxy.class) && clazz.isAnnotationPresent(Aspect.class)){
                            //register aspect
                            AspectProcessor.getInstance().process(clazz);
                        }else if(Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)){
                            //register interceptor
                            InterceptorProcessor.getInstance().register(clazz);
                        }
                    }else{
                        if(clazz.isAnnotationPresent(Aspect.class)){
                            AspectProcessor.getInstance().process(clazz);
                        } else if (Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)) {
                            InterceptorProcessor.getInstance().register(clazz);
                        }
                    }
                } else if (clazz.isAnnotationPresent(Interceptor.class)) {
                    if(Arrays.asList(clazz.getInterfaces()).contains(HandlerInterceptor.class)){
                        //register interceptor
                        InterceptorProcessor.getInstance().register(clazz);
                    }
                }
            })
        );

        //为切面指向的类创建代理
        createProxy();
    }

    private void createProxy() {
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
            aspectProcessor.createProxy(null);
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
        getClasses().stream().filter(e -> e.getName().matches(AspectProcessor.getInstance().toRegExp(className))).forEach(e -> methodScan(e,set));
    }

    public void patternMethodScan(String methodName, Set<Signature> set, boolean patternMatch) {
        int index = methodName.lastIndexOf(".");
        String className = methodName.substring(0,index);
        getClasses().stream().filter(e -> e.getName().equals(className)).forEach(e -> {
            Method[] methods = e.getDeclaredMethods();
            for(Method m:methods){
                if(patternMatch){
                    if(m.getName().matches(AspectProcessor.getInstance().toRegExp(methodName.substring(index + 1)))){
                        set.add(new Signature(m.getParameterCount(),m.getParameterTypes(), className + PATH_SEPARATOR + m.getName()));
                    }
                }else{
                    if(m.getName().equals(methodName.substring(index + 1))){
                        set.add(new Signature(m.getParameterCount(),m.getParameterTypes(), className + PATH_SEPARATOR + m.getName()));
                    }
                }
            }
        });
    }
}
