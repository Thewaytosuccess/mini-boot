package com.mvc.util.aspect;

import com.mvc.entity.method.Signature;
import com.mvc.util.injection.DependencyInjectProcessor;
import com.mvc.util.mapping.HandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class AspectHandler {

    public static void reInject(){
        if(AspectProcessor.reInjected()){
            Set<Class<?>> classes = new HashSet<>();
            Map<String, Class<?>[]> reInjected = AspectProcessor.getReInjected();
            reInjected.forEach((k,v) -> {
                try {
                    classes.add(Class.forName(k));
                    if(v.length > 0){
                        classes.addAll(Arrays.asList(v));
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            if(!classes.isEmpty()){
                //将代理对象重新注入到依赖它的类中
                getClasses().forEach(e -> reInject(e,classes));
            }
        }
    }

    private static void reInject(String className, Set<Class<?>> classes){
        try {
            DependencyInjectProcessor.reInject(Class.forName(className),classes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> getClasses(){
        return HandlerMapping.getClasses();
    }

    public static void createProxy() {
        if(AspectProcessor.rescan()){
            //为携带切面注解的方法生成代理
            Map<Class<?>, Signature> annotationMethodMap = AspectProcessor.getAnnotationMethodMap();
            if(!annotationMethodMap.isEmpty()){
                Set<Class<?>> annotations = annotationMethodMap.keySet();
                List<Signature> methods = new ArrayList<>();
                getClasses().forEach(e -> getAnnotatedMethod(e,annotations,annotationMethodMap, methods));
                AspectProcessor.createProxy(methods);
            }
        }else{
            AspectProcessor.createProxy();
        }
    }

    private static void getAnnotatedMethod(String className, Set<Class<?>> annotations,
                                           Map<Class<?>, Signature> annotationMethodMap,
                                           List<Signature> classes) {
        try {
            Method[] declaredMethods = Class.forName(className).getDeclaredMethods();
            Signature signature;
            Annotation[] declaredAnnotations;
            for(Method m:declaredMethods){
                declaredAnnotations = m.getDeclaredAnnotations();
                for(Annotation a:declaredAnnotations){
                    if(annotations.contains(a.annotationType())){
                        signature = annotationMethodMap.get(a.annotationType());
                        signature.setMethodName(className + PATH_SEPARATOR + m.getName());
                        classes.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                                signature.getMethodName(),signature.getAdviceEnum(),signature.getAdviceMethod()));
                        break;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void methodScan(String className, Set<Signature> list) {
        getClasses().stream().filter(e -> e.equals(className)).forEach(e -> {
            try {
                Method[] methods = Class.forName(e).getDeclaredMethods();
                for(Method m:methods){
                    list.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                            className + PATH_SEPARATOR + m.getName()));
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
    }

    public static void classScan(String packageName, Set<Signature> list) {
        getClasses().stream().filter(e -> e.startsWith(packageName)).forEach(e -> methodScan(e, list));
    }

    public static void patternClassScan(String className, Set<Signature> list) {
        getClasses().stream().filter(e -> e.matches(AspectProcessor.toRegExp(className))).forEach(e -> methodScan(e, list));
    }

    public static void patternMethodScan(String methodName, Set<Signature> list, boolean patternMatch) {
        int index = methodName.lastIndexOf(".");
        String className = methodName.substring(0,index);
        getClasses().stream().filter(e -> e.equals(className)).forEach(e -> {
            try {
                Method[] methods = Class.forName(e).getDeclaredMethods();
                for(Method m:methods){
                    if(patternMatch){
                        if(m.getName().matches(AspectProcessor.toRegExp(methodName.substring(index + 1)))){
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
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
    }
}
