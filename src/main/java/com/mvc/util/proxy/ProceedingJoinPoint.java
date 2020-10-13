package com.mvc.util.proxy;

import com.mvc.entity.method.MethodInfo;
import com.mvc.enums.AdviceEnum;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.util.aspect.AspectProcessor;
import com.mvc.util.injection.DependencyInjectProcessor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class ProceedingJoinPoint {

    private static final Map<MethodInfo,String> BEFORE_MAP = new ConcurrentHashMap<>();

    private static final Map<MethodInfo,String> AFTER_MAP = new ConcurrentHashMap<>();

    private static final Map<MethodInfo,String> AFTER_RETURNING_MAP = new ConcurrentHashMap<>();

    private static final Map<MethodInfo,String> AFTER_THROWING_MAP = new ConcurrentHashMap<>();

    /**
     * 被代理对象
     */
    protected Object target;

    /**
     * 被代理对象的方法
     */
    protected Method method;

    /**
     * 被代理方法的参数
     */
    protected Object[] args;

    /**
     * 是否为jdk动态代理
     */
    protected boolean jdkProxy;

    /**
     * 通过methodProxy.invokeSuper(obj,args)调用父类的方法，可以实现动态代理，
     * 但被代理的对象是重新创建出来的，并不是target，所以target中本来已经注入的属性都为null；
     * @return 父类方法的执行结果
     */
    public Object proceed(){
        try{
            return jdkProxy ? method.invoke(target, args) : target.getClass().getDeclaredMethod(method.getName(),
                    method.getParameterTypes()).invoke(target,args);
        }catch (Exception e){
            throw new RuntimeException();
        }
    }

    public void setMethod(AdviceEnum adviceEnum, MethodInfo info, String proxyMethod){
        switch (adviceEnum){
            case Before:
                if(Objects.isNull(BEFORE_MAP.get(info))){
                    BEFORE_MAP.put(info,proxyMethod);
                }
                break;
            case After:
                AFTER_MAP.put(info,proxyMethod);
                break;
            case AfterReturning:
                AFTER_RETURNING_MAP.put(info,proxyMethod);
                break;
            case AfterThrowing:
                AFTER_THROWING_MAP.put(info,proxyMethod);
                break;
            case Around:
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + adviceEnum);
        }
    }

    protected ProceedingJoinPoint(Object target, MethodInfo info, String[] methods, boolean jdkProxy){
        this.target = target;
        this.jdkProxy = jdkProxy;
        String method = methods[0];
        if(Objects.nonNull(method) && !method.isEmpty()){
            BEFORE_MAP.put(info,method);
        }

        method = methods[1];
        if(Objects.nonNull(method) && !method.isEmpty()){
            AFTER_MAP.put(info,method);
        }

        method = methods[2];
        if(Objects.nonNull(method) && !method.isEmpty()){
            AFTER_RETURNING_MAP.put(info,method);
        }

        method = methods[3];
        if(Objects.nonNull(method) && !method.isEmpty()){
            AFTER_THROWING_MAP.put(info,method);
        }
    }

    protected Object preHandle(){
        String methodName = BEFORE_MAP.get(getMethodInfo());
        if(Objects.nonNull(methodName) && !methodName.isEmpty()){
            Class<?>[] argTypes = new Class[]{this.getClass().getSuperclass().getSuperclass()};
            Object[] args = new Object[]{this};
            return handle(methodName, argTypes, args);
        }else{
            System.out.println("method not found in proxy map = "+method.getName());
        }
        return null;
    }

    protected Object handle(){
        MethodInfo info = getMethodInfo();
        String methodName = BEFORE_MAP.get(info);
        if(Objects.nonNull(methodName) && !methodName.isEmpty()){
            handle(methodName);
        }else{
            System.out.println("method not found in proxy map = "+method.getName());
        }

        Object result = null;
        try{
            result = proceed();
        }catch(Exception e){
            e.printStackTrace();
            methodName = AFTER_THROWING_MAP.get(info);
            if(Objects.nonNull(methodName) && !methodName.isEmpty()){
                handle(methodName);
            }
        }finally{
            methodName = AFTER_RETURNING_MAP.get(info);
            if(Objects.nonNull(methodName) && !methodName.isEmpty()){
                handle(methodName);
            }
        }
        methodName = AFTER_MAP.get(info);
        if(Objects.nonNull(methodName) && !methodName.isEmpty()){
            handle(methodName);
        }
        return result;
    }

    private MethodInfo getMethodInfo() {
        MethodInfo info = new MethodInfo();
        info.setModifiers(method.getModifiers());
        String classImpl = AspectProcessor.getClassImpl(method.getDeclaringClass());
        if(Objects.isNull(classImpl) || classImpl.isEmpty()){
            throw new RuntimeException();
        }
        info.setMethodName(classImpl + ConstantPool.PATH_SEPARATOR + method.getName());
        info.setParameterCount(method.getParameterCount());
        info.setParameterTypes(method.getParameterTypes());
        return info;
    }

    private void handle(String method){
        handle(method, null, null);
    }

    private Object handle(String method, Class<?>[] argTypes,Object[] args){
        if(Objects.nonNull(method) && !method.isEmpty()){
            int index = method.lastIndexOf(PATH_SEPARATOR);
            try {
                Class<?> clazz = Class.forName(method.substring(0, index));
                //从ioc容器中查询实例
                if(Objects.nonNull(args) && Objects.nonNull(argTypes) && args.length > 0 && argTypes.length > 0){
                    return clazz.getDeclaredMethod(method.substring(index + 1),argTypes)
                            .invoke(DependencyInjectProcessor.getInstance(clazz), args);
                }else{
                    return clazz.getDeclaredMethod(method.substring(index + 1)).invoke(DependencyInjectProcessor.getInstance(clazz));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
