package com.mvc.util.aspect;

import com.mvc.annotation.aop.advice.*;
import com.mvc.annotation.aop.aspect.Aspect;
import com.mvc.entity.method.MethodInfo;
import com.mvc.enums.AdviceEnum;
import com.mvc.enums.ModifiersEnum;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.util.proxy.ProceedingJoinPoint;
import com.mvc.util.proxy.cglib.CglibAroundProxy;
import com.mvc.util.proxy.cglib.CglibProxy;
import com.mvc.util.proxy.jdk.JdkProxy;
import com.mvc.util.proxy.jdk.JdkAroundProxy;
import com.mvc.util.injection.DependencyInjectProcessor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * todo 基于类和注解的切面，拦截器，@PostConstruct，@PreDestroy，统一异常处理
 * @author xhzy
 */
public class AspectProcessor {

    /**
     * 实现类和接口的映射
     */
    private static final Map<String,Class<?>[]> CLASS_IMPL_INTERFACES_MAP = new ConcurrentHashMap<>();

    /**
     * 代理对象和连接点的映射
     */
    private static final Map<Class<?>, ProceedingJoinPoint> PROXY_JOIN_POINT_MAP = new ConcurrentHashMap<>();

    /**
     * 解析execution表达式
     * @param clazz Class Object
     *  1.jdk动态代理
     *  2.cglib动态代理
     *  3.javassist动态代理
     */
    public static void process(Class<?> clazz){
        if(clazz.isAnnotationPresent(Aspect.class)){
            Method[] declaredMethods = clazz.getDeclaredMethods();
            String methodName;
            for(Method m:declaredMethods){
                methodName = clazz.getName() + ConstantPool.PATH_SEPARATOR + m.getName();
                if(m.isAnnotationPresent(Before.class)){
                    MethodInfo info = parseExecutionExpression(m.getAnnotation(Before.class).execution());
                    if(Objects.nonNull(info)){
                        createProxy(methodName,info, AdviceEnum.Before);
                    }
                }else if(m.isAnnotationPresent(Around.class)){
                    MethodInfo info = parseExecutionExpression(m.getAnnotation(Around.class).execution());
                    if(Objects.nonNull(info)){
                        createProxy(methodName,info, AdviceEnum.Around);
                    }
                }else if(m.isAnnotationPresent(After.class)){
                    MethodInfo info = parseExecutionExpression(m.getAnnotation(After.class).execution());
                    if(Objects.nonNull(info)){
                        createProxy(methodName,info, AdviceEnum.After);
                    }
                }else if(m.isAnnotationPresent(AfterReturning.class)){
                    MethodInfo info = parseExecutionExpression(m.getAnnotation(AfterReturning.class).execution());
                    if(Objects.nonNull(info)){
                        createProxy(methodName,info, AdviceEnum.AfterReturning);
                    }
                }else if(m.isAnnotationPresent(AfterThrowing.class)){
                    MethodInfo info = parseExecutionExpression(m.getAnnotation(AfterThrowing.class).execution());
                    if(Objects.nonNull(info)){
                        createProxy(methodName,info, AdviceEnum.AfterThrowing);
                    }
                }
            }
        }
    }

    public static boolean reInjected(){
        return !CLASS_IMPL_INTERFACES_MAP.isEmpty();
    }

    public static Map<String,Class<?>[]> getReInjected(){
        return CLASS_IMPL_INTERFACES_MAP;
    }

    public static String getClassImpl(Class<?> interfaceClass){
        //jdk proxy
        for(Map.Entry<String,Class<?>[]> e: CLASS_IMPL_INTERFACES_MAP.entrySet()){
            for(Class<?> c:e.getValue()){
                if(c == interfaceClass){
                    return e.getKey();
                }
            }
        }

        //cglib proxy
        for(Map.Entry<String,Class<?>[]> e: CLASS_IMPL_INTERFACES_MAP.entrySet()){
            try {
                if(Class.forName(e.getKey()) == interfaceClass){
                    return e.getKey();
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    private static void createProxy(String proxyMethod, MethodInfo info, AdviceEnum adviceEnum){
        try {
            String targetMethod = info.getMethodName();
            Class<?> targetClass = Class.forName(targetMethod.substring(0, targetMethod.lastIndexOf(PATH_SEPARATOR)));
            Object target = DependencyInjectProcessor.getInstance(targetClass);

            if(Objects.nonNull(target)){
                //判断是否已经创建过代理
                if(targetClass != target.getClass()){
                    //已经创建过代理类
                    PROXY_JOIN_POINT_MAP.get(target.getClass()).setMethod(adviceEnum,info,proxyMethod);
                    return;
                }

                boolean jdkProxy = false;
                ClassLoader classLoader = null;
                Class<?>[] interfaces = targetClass.getInterfaces();
                if(interfaces.length > 0){
                    jdkProxy = true;
                    classLoader = targetClass.getClassLoader();
                }

                //建立实现类和接口的映射
                CLASS_IMPL_INTERFACES_MAP.put(targetClass.getName(),interfaces);
                String[] methods = new String[4];
                //创建代理对象
                Object proxy;
                switch (adviceEnum){
                    case Before:
                        methods[0] = proxyMethod;
                        proxy = createProxy(target,info,methods,classLoader,interfaces,jdkProxy);
                        break;
                    case Around:
                        methods[0] = proxyMethod;
                        proxy = createAroundProxy(target,info,methods,classLoader,interfaces,jdkProxy);
                        break;
                    case AfterReturning:
                        methods[2] = proxyMethod;
                        proxy = createProxy(target,info,methods,classLoader,interfaces,jdkProxy);
                        break;
                    case AfterThrowing:
                        methods[3] = proxyMethod;
                        proxy = createProxy(target,info,methods,classLoader,interfaces,jdkProxy);
                        break;
                    case After:
                        methods[1] = proxyMethod;
                        proxy = createProxy(target,info,methods,classLoader,interfaces,jdkProxy);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + adviceEnum);
                }
                //将代理对象注入到ioc容器
                DependencyInjectProcessor.replace(targetClass,proxy);
            }else{
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object createProxy(Object target, MethodInfo info, String[] methods, ClassLoader classLoader,
                                    Class<?>[] interfaces, boolean jdkProxy){
        Object proxy;
        if(jdkProxy){
            JdkProxy proxyClass = new JdkProxy(target,info,methods, true);
            proxy = Proxy.newProxyInstance(classLoader, interfaces, proxyClass);
            PROXY_JOIN_POINT_MAP.put(proxy.getClass(),proxyClass);
        }else{
            CglibProxy proxyClass = new CglibProxy(target,info,methods, false);
            proxy = proxyClass.getProxy();
            PROXY_JOIN_POINT_MAP.put(proxy.getClass(),proxyClass);
        }
        return proxy;
    }

    private static Object createAroundProxy(Object target, MethodInfo info, String[] methods, ClassLoader classLoader,
                                      Class<?>[] interfaces, boolean jdkProxy){
        Object proxy;
        if(jdkProxy){
            JdkProxy proxyClass = new JdkAroundProxy(target,info,methods, true);
            proxy = Proxy.newProxyInstance(classLoader, interfaces, proxyClass);
            PROXY_JOIN_POINT_MAP.put(proxy.getClass(),proxyClass);
        }else{
            CglibProxy proxyClass = new CglibAroundProxy(target,info,methods, false);
            proxy = proxyClass.getProxy();
            PROXY_JOIN_POINT_MAP.put(proxy.getClass(),proxyClass);
        }
        return proxy;
    }

    /**
     * 解析execution表达式
     * @param execution execution表达式
     * eg:[public String com.mvc.Class.method(pType pName...)]
     */
    private static MethodInfo parseExecutionExpression(String execution) {
        if(!execution.isEmpty()){
            String[] split = execution.split(" ");
            if(split.length != ConstantPool.TWO){
                throw new IllegalArgumentException();
            }
            MethodInfo info = new MethodInfo();
            if(!split[0].isEmpty()){
                setModifiers(info,split[0]);
            }else{
                throw new IllegalArgumentException();
            }

            String param = split[1];
            if(!param.isEmpty()){
                setNameAndParameters(info,param);
            }else{
                throw new IllegalArgumentException();
            }
            return info;
        }
        return null;
    }

    private static void setNameAndParameters(MethodInfo info, String s) {
        String[] split = s.split("\\(");
        if(split.length == ConstantPool.TWO){
            info.setMethodName(split[0]);
            if(split[1].endsWith(ConstantPool.RIGHT_BRACKET)){
                String param = split[1].substring(0,split[1].length() - 1);
                if(param.isEmpty()){
                    info.setParameterCount(0);
                    info.setCompared(false);
                }else if(ConstantPool.ANY_PARAM.equals(param)){
                    info.setCompared(false);
                }
            }else{
                throw new IllegalArgumentException();
            }
        }else{
            throw new IllegalArgumentException();
        }
    }

    private static void setModifiers(MethodInfo info,String s){
        if(ModifiersEnum.PUBLIC.getKey().equals(s)){
            info.setModifiers(Modifier.PUBLIC);
        }else if(ModifiersEnum.PRIVATE.getKey().equals(s)){
            info.setModifiers(Modifier.PRIVATE);
        }else if(ModifiersEnum.PROTECTED.getKey().equals(s)){
            info.setModifiers(Modifier.PROTECTED);
        }else if(ModifiersEnum.NULL.getKey().equals(s)){
            info.setModifiers(0);
        }
    }

}
