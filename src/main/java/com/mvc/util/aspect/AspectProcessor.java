package com.mvc.util.aspect;

import com.mvc.annotation.aop.advice.*;
import com.mvc.annotation.aop.aspect.Aspect;
import com.mvc.entity.method.MethodInfo;
import com.mvc.entity.method.Signature;
import com.mvc.enums.AdviceEnum;
import com.mvc.enums.ModifiersEnum;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.util.exception.ControllerAdviceHandler;
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
import java.util.stream.Collectors;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * todo 基于类和注解的切面，拦截器，统一异常处理
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
    private static final Map<Class<?>,Signature> ANNOTATION_METHOD_MAP = new ConcurrentHashMap<>();

    /**
     * 类和其内所有带切面注解的方法的映射
     */
    private static final Map<Class<?>,List<Signature>> CLASS_METHOD_MAP = new ConcurrentHashMap<>();

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
                    parseExpression(m.getAnnotation(Before.class).value(),adviceMethod,AdviceEnum.Before);
                }else if(m.isAnnotationPresent(Around.class)){
                    parseExpression(m.getAnnotation(Around.class).value(), adviceMethod, AdviceEnum.Around);
                }else if(m.isAnnotationPresent(After.class)){
                    parseExpression(m.getAnnotation(After.class).value(), adviceMethod, AdviceEnum.After);
                }else if(m.isAnnotationPresent(AfterReturning.class)){
                    parseExpression(m.getAnnotation(AfterReturning.class).value(), adviceMethod, AdviceEnum.AfterReturning);
                }else if(m.isAnnotationPresent(AfterThrowing.class)){
                    parseExpression(m.getAnnotation(AfterThrowing.class).value(), adviceMethod, AdviceEnum.AfterThrowing);
                }
            }
        }
    }

    public static void createProxy(List<Signature> methods) {
        methods.forEach(AspectProcessor::buildClassMethodMap);
        createProxy();
    }

    public static void createProxy(){
        //统一异常处理
        ControllerAdviceHandler.handle().forEach(AspectProcessor::buildClassMethodMap);
        CLASS_METHOD_MAP.forEach(AspectProcessor::createProxy);
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

    public static Map<Class<?>,Signature> getAnnotationMethodMap(){ return ANNOTATION_METHOD_MAP; }

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

    private static void createProxy(Class<?> targetClass,List<Signature> list){
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
                for(Signature e:list){
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

    private static Object createProxy(Object target, List<Signature> info, ClassLoader classLoader,
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

    private static Object createAroundProxy(Object target, List<Signature> methods, ClassLoader classLoader,
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
     * eg:[public com.mvc.Class.method(pType pName...)]
     */
    private static void parseExpression(String execution, String adviceMethod, AdviceEnum adviceEnum) {
        if(!execution.isEmpty()){
            if(execution.startsWith(ConstantPool.EXECUTION_PREFIX) && execution.endsWith(ConstantPool.RIGHT_BRACKET)){
                parseExecutionExpression(execution).forEach(e -> {
                    e.setAdviceMethod(adviceMethod);
                    e.setAdviceEnum(adviceEnum);
                    buildClassMethodMap(e);
                    System.out.println("execution method = "+e);
                });
            }else if(execution.startsWith(ConstantPool.ANNOTATION_PREFIX) && execution.endsWith(ConstantPool.RIGHT_BRACKET)){
                parseAnnotationExpression(execution,adviceMethod,adviceEnum);
            }
        }
    }

    private static Set<Signature> parseExecutionExpression(String execution){
        execution = execution.substring(10, execution.length() - 1);
        String[] split = execution.split(" ");
        if(split.length != ConstantPool.TWO){
            throw new IllegalArgumentException();
        }

        String param = split[1];
        if(!param.isEmpty()){
            return setMethodNameAndParameters(param);
        }else{
            throw new IllegalArgumentException();
        }
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
                Signature signature = new Signature();
                signature.setAdviceMethod(methodName);
                signature.setAdviceEnum(adviceEnum);
                ANNOTATION_METHOD_MAP.put(clazz,signature);
            }else{
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String toRegExp(String exp){
        StringBuilder builder = new StringBuilder("^");
        String[] split = exp.split("\\.");
        for(String str:split){
            builder.append(str).append("\\.");
        }
        split = builder.toString().split("\\*");
        builder.delete(0,builder.length());
        for(int i=0,len=split.length ; i < len - 1; ++i){
            builder.append(split[i]).append("+\\w+");
        }
        return builder.toString();
    }

    private static Set<Signature> setMethodNameAndParameters(String s) {
        String[] split = s.split("\\(");
        if(split.length == ConstantPool.TWO){
            Set<Signature> methods = new HashSet<>();
            String[] parts = getParts(split);
            int len = parts.length;
            String className = parts[len - 2];
            String methodName = parts[len - 1];

            if(className.contains(ConstantPool.ANY)){
                if(className.equals(ConstantPool.ANY)){
                    int index = split[0].length() - className.length() - methodName.length() - 2;
                    AspectHandler.classScan(split[0].substring(0,index), methods);
                }else{
                    //pattern match
                    AspectHandler.patternClassScan(split[0].substring(0,split[0].lastIndexOf(".")), methods);
                }
            }

            if(methodName.contains(ConstantPool.ANY)){
                if(methodName.equals(ConstantPool.ANY)){
                    //类下的所有方法
                    if(methods.isEmpty()){
                        try {
                            Class<?> clazz = Class.forName(split[0].substring(0,split[0].lastIndexOf(".")));
                            AspectHandler.methodScan(clazz, methods);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    //pattern match
                    if(methods.isEmpty()){
                        AspectHandler.patternMethodScan(split[0], methods,true);
                    }else{
                        methods = methods.stream().filter(e -> e.getMethodName().matches(
                                toRegExp(split[0]))).collect(Collectors.toSet());
                    }
                }
            }else {
                if(methods.isEmpty()){
                    AspectHandler.patternMethodScan(split[0], methods,false);
                }else{
                    methods = methods.stream().filter(e -> e.getMethodName().endsWith(methodName)).
                            collect(Collectors.toSet());
                }
            }

            String param = split[1].substring(0, split[1].length() - 1);
            if (param.isEmpty()) {
                methods = methods.stream().filter(e -> e.getParameterCount() == 0).collect(Collectors.toSet());
            } else if (ConstantPool.ANY_PARAM.equals(param)) {
                //类下的指定方法
                if(methods.isEmpty()){
                    AspectHandler.patternMethodScan(split[0], methods,false);
                }
            } else {
                //根据参数精确匹配
                System.out.println("TODO");
            }
            return methods;
        }else{
            throw new IllegalArgumentException();
        }
    }

    private static String[] getParts(String[] split) {
        if(!split[1].endsWith(ConstantPool.RIGHT_BRACKET)){
            throw new IllegalArgumentException();
        }

        String[] parts = split[0].split("\\.");
        if(parts.length <= ConstantPool.TWO){
            throw new IllegalArgumentException();
        }
        return parts;
    }

    private static void buildClassMethodMap(Signature signature){
        try{
            String methodName = signature.getMethodName();
            Class<?> key = Class.forName(methodName.substring(0, methodName.lastIndexOf(PATH_SEPARATOR)));
            List<Signature> methods = CLASS_METHOD_MAP.get(key);
            if(Objects.isNull(methods)){
                methods = new ArrayList<>();
            }
            methods.add(signature);
            CLASS_METHOD_MAP.put(key,methods);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Deprecated
    public static void setModifiers(MethodInfo info,String s){
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
