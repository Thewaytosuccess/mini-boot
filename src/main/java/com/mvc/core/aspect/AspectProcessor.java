package com.mvc.core.aspect;

import com.mvc.annotation.aop.advice.*;
import com.mvc.entity.method.MethodInfo;
import com.mvc.entity.method.Signature;
import com.mvc.enums.AdviceEnum;
import com.mvc.enums.ExceptionEnum;
import com.mvc.enums.ModifiersEnum;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.core.task.async.AsyncTaskManager;
import com.mvc.core.exception.ControllerAdviceHandler;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.IocContainer;
import com.mvc.core.proxy.ProceedingJoinPoint;
import com.mvc.core.proxy.cglib.CglibAroundProxy;
import com.mvc.core.proxy.cglib.CglibProxy;
import com.mvc.core.proxy.jdk.JdkProxy;
import com.mvc.core.proxy.jdk.JdkAroundProxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class AspectProcessor {

    private static final AspectProcessor PROCESSOR = new AspectProcessor();

    private AspectProcessor(){}

    public static AspectProcessor getInstance(){
        return PROCESSOR;
    }

    /**
     * 实现类和接口的映射
     */
    private Map<Class<?>,Class<?>[]> classImplInterfacesMap;

    /**
     * 代理对象和连接点的映射
     */
    private Map<Class<?>, ProceedingJoinPoint> proxyJoinPointMap;

    /**
     * 注解和切面方法的映射
     */
    private Map<Class<?>,Signature> annotationMethodMap;

    /**
     * 类和其内所有带切面注解的方法的映射
     */
    private Map<Class<?>,List<Signature>> classMethodMap;

    /**
     * 解析execution表达式
     * @param clazz Class Object
     *  1.jdk动态代理
     *  2.cglib动态代理
     *  3.javassist动态代理
     */
    public void process(Class<?> clazz) {
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

    public void createProxy(List<Signature> methods) {
        if(Objects.nonNull(methods)){
            methods.forEach(this::buildClassMethodMap);
        }
        createProxy();
    }

    public void createProxy(){
        //统一异常处理
        ControllerAdviceHandler.getInstance().handle().forEach(this::buildClassMethodMap);
        //异步任务扫描
        AsyncTaskManager.getInstance().scan().forEach(this::buildClassMethodMap);

        if(Objects.isNull(classMethodMap)){
            return;
        }
        classMethodMap.forEach(this::createProxy);
    }

    public boolean reInjected(){
        return Objects.nonNull(classImplInterfacesMap) && !classImplInterfacesMap.isEmpty();
    }

    public boolean rescan(){
        return Objects.nonNull(annotationMethodMap) && !annotationMethodMap.isEmpty();
    }

    public Map<Class<?>,Class<?>[]> getReInjected(){
        return classImplInterfacesMap;
    }

    public Map<Class<?>,Signature> getAnnotationMethodMap(){ return annotationMethodMap; }

    public String getClassImpl(Class<?> interfaceOrSuper){
        //jdk proxy
        Set<Map.Entry<Class<?>, Class<?>[]>> entries = classImplInterfacesMap.entrySet();
        Optional<Map.Entry<Class<?>, Class<?>[]>> first = entries.stream().filter(e -> Arrays.stream(
                e.getValue()).anyMatch(c -> c == interfaceOrSuper)).findAny();
        if(first.isPresent()){
            return first.get().getKey().getName();
        }

        //cglib proxy
        first = entries.stream().filter(e -> e.getKey() == interfaceOrSuper).findFirst();
        return first.map(e -> e.getKey().getName()).orElse(null);
    }

    private void createProxy(Class<?> targetClass,List<Signature> list){
        try {
            Object target = IocContainer.getInstance().getClassInstance(targetClass);
            if(Objects.nonNull(target)){
                //判断是否已经创建过代理
                if(targetClass != target.getClass()){
                    //已经创建过代理类
                    proxyJoinPointMap.get(target.getClass()).setMethod(list);
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
                if(Objects.isNull(classImplInterfacesMap)){
                    classImplInterfacesMap = new HashMap<>(16);
                }
                classImplInterfacesMap.put(targetClass,interfaces);

                //创建代理对象
                boolean flag = list.stream().anyMatch(e -> e.getAdviceEnum() == AdviceEnum.Around);
                Object proxy = flag ? createAroundProxy(target,list,classLoader,interfaces,jdkProxy) :
                        createProxy(target,list,classLoader,interfaces,jdkProxy);
                //将代理对象注入到ioc容器
                IocContainer.getInstance().addInstance(targetClass,proxy);
            }else{
                throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
            }
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    private Object createProxy(Object target, List<Signature> info, ClassLoader classLoader,
                                      Class<?>[] interfaces, boolean jdkProxy){
        proxyJoinPointMap = getProxyJoinPointMap(proxyJoinPointMap);
        Object proxy;
        if(jdkProxy){
            JdkProxy proxyClass = new JdkProxy(target,info,true);
            proxy = Proxy.newProxyInstance(classLoader, interfaces, proxyClass);
            proxyJoinPointMap.put(proxy.getClass(),proxyClass);
        }else{
            CglibProxy proxyClass = new CglibProxy(target,info, false);
            proxy = proxyClass.getProxy();
            proxyJoinPointMap.put(proxy.getClass(),proxyClass);
        }
        return proxy;
    }

    private Object createAroundProxy(Object target, List<Signature> methods, ClassLoader classLoader,
                                            Class<?>[] interfaces, boolean jdkProxy){
        proxyJoinPointMap = getProxyJoinPointMap(proxyJoinPointMap);
        Object proxy;
        if(jdkProxy){
            JdkProxy proxyClass = new JdkAroundProxy(target,methods, true);
            proxy = Proxy.newProxyInstance(classLoader, interfaces, proxyClass);
            proxyJoinPointMap.put(proxy.getClass(),proxyClass);
        }else{
            CglibProxy proxyClass = new CglibAroundProxy(target,methods, false);
            proxy = proxyClass.getProxy();
            proxyJoinPointMap.put(proxy.getClass(),proxyClass);
        }
        return proxy;
    }

    private Map<Class<?>, ProceedingJoinPoint> getProxyJoinPointMap(Map<Class<?>, ProceedingJoinPoint> map){
        return Objects.isNull(map) ? new HashMap<>() : map;
    }

    /**
     * 解析execution表达式
     * @param execution execution表达式
     * eg:[public com.mvc.Class.method(pType pName...)]
     */
    private void parseExpression(String execution, String adviceMethod, AdviceEnum adviceEnum) {
        if(!execution.isEmpty()){
            if(execution.startsWith(ConstantPool.EXECUTION_PREFIX) && execution.endsWith(ConstantPool.RIGHT_BRACKET)){
                parseExecutionExpression(execution).forEach(e -> {
                    e.setAdviceMethod(adviceMethod);
                    e.setAdviceEnum(adviceEnum);
                    buildClassMethodMap(e);
                    //System.out.println("execution method = "+e);
                });
            }else if(execution.startsWith(ConstantPool.ANNOTATION_PREFIX) && execution.endsWith(ConstantPool.RIGHT_BRACKET)){
                parseAnnotationExpression(execution,adviceMethod,adviceEnum);
            }
        }
    }

    private Set<Signature> parseExecutionExpression(String execution){
        execution = execution.substring(10, execution.length() - 1);
        String[] split = execution.split(" ");
        if(split.length != ConstantPool.TWO){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }

        String param = split[1];
        if(!param.isEmpty()){
            return setMethodNameAndParameters(param);
        }else{
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
    }

    private void parseAnnotationExpression(String execution, String adviceMethod, AdviceEnum adviceEnum) {
        execution = execution.substring(12, execution.length() - 1);
        if(execution.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }

        try {
            //扫描包含此注解的方法创建aop代理
            Class<?> clazz = Class.forName(execution);
            if(clazz.isAnnotation()){
                Signature signature = new Signature();
                signature.setAdviceMethod(adviceMethod);
                signature.setAdviceEnum(adviceEnum);

                if(Objects.isNull(annotationMethodMap)){
                    annotationMethodMap = new HashMap<>(16);
                }
                annotationMethodMap.put(clazz,signature);
            }else{
                throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
            }
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    public String toRegExp(String exp){
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

    private Set<Signature> setMethodNameAndParameters(String s) {
        String[] split = s.split("\\(");
        if(split.length == ConstantPool.TWO){
            Set<Signature> methods = new HashSet<>();
            String[] parts = getParts(split);
            int len = parts.length;
            String className = parts[len - 2];
            String methodName = parts[len - 1];

            AspectHandler aspectHandler = AspectHandler.getInstance();
            if(className.contains(ConstantPool.ANY)){
                if(className.equals(ConstantPool.ANY)){
                    int index = split[0].length() - className.length() - methodName.length() - 2;
                    aspectHandler.classScan(split[0].substring(0,index), methods);
                }else{
                    //pattern match
                    aspectHandler.patternClassScan(split[0].substring(0,split[0].lastIndexOf(".")), methods);
                }
            }

            if(methodName.contains(ConstantPool.ANY)){
                if(methodName.equals(ConstantPool.ANY)){
                    //类下的所有方法
                    if(methods.isEmpty()){
                        try {
                            Class<?> clazz = Class.forName(split[0].substring(0,split[0].lastIndexOf(".")));
                            aspectHandler.methodScan(clazz, methods);
                        } catch (ClassNotFoundException e) {
                            throw new ExceptionWrapper(e);
                        }
                    }
                }else{
                    //pattern match
                    if(methods.isEmpty()){
                        aspectHandler.patternMethodScan(split[0], methods,true);
                    }else{
                        methods = methods.stream().filter(e -> e.getMethodName().matches(
                                toRegExp(split[0]))).collect(Collectors.toSet());
                    }
                }
            }else {
                if(methods.isEmpty()){
                    aspectHandler.patternMethodScan(split[0], methods,false);
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
                    aspectHandler.patternMethodScan(split[0], methods,false);
                }
            } else {
                //根据参数精确匹配
                System.out.println("TODO");
            }
            return methods;
        }else{
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
    }

    private String[] getParts(String[] split) {
        if(!split[1].endsWith(ConstantPool.RIGHT_BRACKET)){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }

        String[] parts = split[0].split("\\.");
        if(parts.length <= ConstantPool.TWO){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
        return parts;
    }

    private void buildClassMethodMap(Signature signature){
        try{
            String methodName = signature.getMethodName();
            Class<?> key = Class.forName(methodName.substring(0, methodName.lastIndexOf(PATH_SEPARATOR)));
            if(Objects.isNull(classMethodMap)){
                classMethodMap = new HashMap<>();
            }

            List<Signature> methods = classMethodMap.get(key);
            if(Objects.isNull(methods)){
                methods = new ArrayList<>();
            }
            methods.add(signature);
            classMethodMap.put(key,methods);
        }catch (Exception e){
            throw new ExceptionWrapper(e);
        }
    }

    @Deprecated
    public void setModifiers(MethodInfo info,String s){
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
