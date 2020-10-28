package com.mvc.util.aspect;

import com.mvc.entity.method.Signature;
import com.mvc.util.injection.IocContainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * todo 异步任务，定时任务
 * @author xhzy
 */
public class AspectHandler {

    private static final AspectHandler HANDLER = new AspectHandler();

    private AspectHandler(){}

    public static AspectHandler getInstance(){ return HANDLER; }

    private List<Class<?>> getClasses(){
        return IocContainer.getInstance().getClasses();
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
                                    List<Signature> classes) {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        Signature signature;
        Annotation[] declaredAnnotations;
        for(Method m:declaredMethods){
            declaredAnnotations = m.getDeclaredAnnotations();
            for(Annotation a:declaredAnnotations){
                if(annotations.contains(a.annotationType())){
                    signature = annotationMethodMap.get(a.annotationType());
                    signature.setMethodName(clazz.getName() + PATH_SEPARATOR + m.getName());
                    classes.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                            signature.getMethodName(),signature.getAdviceEnum(),signature.getAdviceMethod()));
                    break;
                }
            }
        }
    }

    public void methodScan(Class<?> clazz, Set<Signature> set) {
        Method[] methods = clazz.getDeclaredMethods();
        for(Method m:methods){
            set.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                    clazz.getName() + PATH_SEPARATOR + m.getName()));
        }
    }

    public void classScan(String packageName, Set<Signature> list) {
        getClasses().stream().filter(e -> e.getName().startsWith(packageName)).forEach(e -> methodScan(e, list));
    }

    public void patternClassScan(String className, Set<Signature> list) {
        getClasses().stream().filter(e -> e.getName().matches(AspectProcessor.getInstance().toRegExp(className))).
                forEach(e -> methodScan(e, list));
    }

    public void patternMethodScan(String methodName, Set<Signature> list, boolean patternMatch) {
        int index = methodName.lastIndexOf(".");
        String className = methodName.substring(0,index);
        getClasses().stream().filter(e -> e.getName().equals(className)).forEach(e -> {
            Method[] methods = e.getDeclaredMethods();
            for(Method m:methods){
                if(patternMatch){
                    if(m.getName().matches(AspectProcessor.getInstance().toRegExp(methodName.substring(index + 1)))){
                        list.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                                className + PATH_SEPARATOR + m.getName()));
                    }
                }else{
                    if(m.getName().equals(methodName.substring(index + 1))){
                        list.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                                className + PATH_SEPARATOR + m.getName()));
                    }
                }
            }
        });
    }
}
