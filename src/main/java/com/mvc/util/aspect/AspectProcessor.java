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
import java.util.*;
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
     * 注解和切面方法的映射
     */
    private static final Map<Class<?>,MethodInfo> ANNOTATION_METHOD_MAP = new ConcurrentHashMap<>();

    /**
     * 类和其内所有带切面注解的方法的映射
     */
    private static final Map<Class<?>,List<MethodInfo>> CLASS_METHOD_MAP = new ConcurrentHashMap<>();

    /**
     * 解析execution表达式
     * @param clazz Class Object
     *  1.jdk动态代理
     *  2.cglib动态代理
     *  3.javassist动态代理
     */
    public static void process(Class<?> clazz) {
        if(clazz.isAnnotationPresent(Aspect.class)){
            Method[] declaredMethods = clazz.getDeclaredMethods();
            String adviceMethod;
            for(Method m:declaredMethods){
                adviceMethod = clazz.getName() + ConstantPool.PATH_SEPARATOR + m.getName();
                if(m.isAnnotationPresent(Before.class)){
                    MethodInfo info = parseExpression(m.getAnnotation(Before.class).value(),adviceMethod,AdviceEnum.Before);
                    if(Objects.nonNull(info)){
                        buildClassMethodMap(info);
                    }
                }else if(m.isAnnotationPresent(Around.class)){
                    MethodInfo info = parseExpression(m.getAnnotation(Around.class).value(), adviceMethod, AdviceEnum.Around);
                    if(Objects.nonNull(info)){
                        buildClassMethodMap(info);
                    }
                }else if(m.isAnnotationPresent(After.class)){
                    MethodInfo info = parseExpression(m.getAnnotation(After.class).value(), adviceMethod,
                            AdviceEnum.After);
                    if(Objects.nonNull(info)){
                        buildClassMethodMap(info);
                    }
                }else if(m.isAnnotationPresent(AfterReturning.class)){
                    MethodInfo info = parseExpression(m.getAnnotation(AfterReturning.class).value(),
                            adviceMethod, AdviceEnum.AfterReturning);
                    if(Objects.nonNull(info)){
                        buildClassMethodMap(info);
                    }
                }else if(m.isAnnotationPresent(AfterThrowing.class)){
                    MethodInfo info = parseExpression(m.getAnnotation(AfterThrowing.class).value(),
                            adviceMethod, AdviceEnum.AfterThrowing);
                    if(Objects.nonNull(info)){
                        buildClassMethodMap(info);
                    }
                }
            }
        }
    }

    private static void buildClassMethodMap(MethodInfo info){
        try{
            String methodName = info.getMethodName();
            Class<?> key = Class.forName(methodName.substring(0, methodName.lastIndexOf(PATH_SEPARATOR)));
            List<MethodInfo> methods = CLASS_METHOD_MAP.get(key);
            if(Objects.isNull(methods)){
                methods = new ArrayList<>();
            }
            methods.add(info);
            CLASS_METHOD_MAP.put(key,methods);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean reInjected(){
        return !CLASS_IMPL_INTERFACES_MAP.isEmpty();
    }

    public static boolean rescan(){
        return !ANNOTATION_METHOD_MAP.isEmpty();
    }

    public static Map<String,Class<?>[]> getReInjected(){
        return CLASS_IMPL_INTERFACES_MAP;
    }

    public static Map<Class<?>,MethodInfo> getAnnotationMethodMap(){ return ANNOTATION_METHOD_MAP; }

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

    private static void createProxy(Class<?> targetClass,List<MethodInfo> list){
        try {
            Object target = DependencyInjectProcessor.getInstance(targetClass);
            if(Objects.nonNull(target)){
                //判断是否已经创建过代理
                if(targetClass != target.getClass()){
                    //已经创建过代理类
                    PROXY_JOIN_POINT_MAP.get(target.getClass()).setMethod(list);
                    return;
                }

                boolean flag = false;
                for(MethodInfo e:list){
                    if(e.getAdviceEnum() == AdviceEnum.Around){
                        flag = true;
                        break;
                    }
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
                //创建代理对象
                Object proxy;
                if(flag){
                    proxy = createAroundProxy(target,list,classLoader,interfaces,jdkProxy);
                }else{
                    proxy = createProxy(target,list,classLoader,interfaces,jdkProxy);
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

    private static Object createProxy(Object target, List<MethodInfo> info, ClassLoader classLoader,
                                      Class<?>[] interfaces, boolean jdkProxy){
        Object proxy;
        if(jdkProxy){
            JdkProxy proxyClass = new JdkProxy(target,info,true);
            proxy = Proxy.newProxyInstance(classLoader, interfaces, proxyClass);
            PROXY_JOIN_POINT_MAP.put(proxy.getClass(),proxyClass);
        }else{
            CglibProxy proxyClass = new CglibProxy(target,info, false);
            proxy = proxyClass.getProxy();
            PROXY_JOIN_POINT_MAP.put(proxy.getClass(),proxyClass);
        }
        return proxy;
    }

    private static Object createAroundProxy(Object target, List<MethodInfo> methods, ClassLoader classLoader,
                                            Class<?>[] interfaces, boolean jdkProxy){
        Object proxy;
        if(jdkProxy){
            JdkProxy proxyClass = new JdkAroundProxy(target,methods, true);
            proxy = Proxy.newProxyInstance(classLoader, interfaces, proxyClass);
            PROXY_JOIN_POINT_MAP.put(proxy.getClass(),proxyClass);
        }else{
            CglibProxy proxyClass = new CglibAroundProxy(target,methods, false);
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
    private static MethodInfo parseExpression(String execution, String adviceMethod, AdviceEnum adviceEnum) {
        if(!execution.isEmpty()){
            if(execution.startsWith(ConstantPool.EXECUTION_PREFIX) && execution.endsWith(ConstantPool.RIGHT_BRACKET)){
                MethodInfo info = parseExecutionExpression(execution);
                info.setAdviceMethod(adviceMethod);
                info.setAdviceEnum(adviceEnum);
                return info;
            }else if(execution.startsWith(ConstantPool.ANNOTATION_PREFIX) && execution.endsWith(ConstantPool.RIGHT_BRACKET)){
                parseAnnotationExpression(execution,adviceMethod,adviceEnum);
            }
        }
        return null;
    }

    private static MethodInfo parseExecutionExpression(String execution){
        execution = execution.substring(execution.indexOf(ConstantPool.EXECUTION_PREFIX + 1), execution.length() - 1);
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

    private static void parseAnnotationExpression(String execution, String methodName, AdviceEnum adviceEnum) {
        execution = execution.substring(12, execution.length() - 1);
        if(execution.isEmpty()){
            throw new IllegalArgumentException();
        }

        try {
            //扫描包含此注解的方法创建aop代理
            Class<?> clazz = Class.forName(execution);
            if(clazz.isAnnotation()){
                MethodInfo info = new MethodInfo();
                info.setAdviceMethod(methodName);
                info.setAdviceEnum(adviceEnum);
                ANNOTATION_METHOD_MAP.put(clazz,info);
            }else{
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static void createProxy(List<MethodInfo> methods) {
        methods.forEach(AspectProcessor::buildClassMethodMap);
        CLASS_METHOD_MAP.forEach(AspectProcessor::createProxy);
    }
}
