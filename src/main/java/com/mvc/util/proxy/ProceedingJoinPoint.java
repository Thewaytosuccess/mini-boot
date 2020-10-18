package com.mvc.util.proxy;

import com.mvc.entity.method.Signature;
import com.mvc.enums.constant.ConstantPool;
import com.mvc.util.aspect.AspectProcessor;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.injection.DependencyInjectProcessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class ProceedingJoinPoint {

    private static final Map<Signature,String> BEFORE_MAP = new ConcurrentHashMap<>();

    private static final Map<Signature,String> AFTER_MAP = new ConcurrentHashMap<>();

    private static final Map<Signature,String> AFTER_RETURNING_MAP = new ConcurrentHashMap<>();

    private static final Map<Signature,String> AFTER_THROWING_MAP = new ConcurrentHashMap<>();

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
        Signature signature = getSignature();
        try{
            return jdkProxy ? method.invoke(target, args) : target.getClass().getDeclaredMethod(method.getName(),
                    method.getParameterTypes()).invoke(target,args);
        } catch (Exception e){
            return afterThrowingHandle(signature,e);
        } finally {
            afterHandle(signature);
            afterReturningHandle(signature);
        }
    }

    public void setMethod(List<Signature> methods){
        methods.forEach(e -> {
            switch (e.getAdviceEnum()){
                case Before:
                    //remove duplicate
                    if(Objects.isNull(BEFORE_MAP.get(e))){
                        BEFORE_MAP.put(e,e.getAdviceMethod());
                    }
                    break;
                case After:
                    AFTER_MAP.put(e,e.getAdviceMethod());
                    break;
                case AfterReturning:
                    AFTER_RETURNING_MAP.put(e,e.getAdviceMethod());
                    break;
                case AfterThrowing:
                    AFTER_THROWING_MAP.put(e,e.getAdviceMethod());
                    break;
                case Around:
                    BEFORE_MAP.put(e,e.getAdviceMethod());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + e.getAdviceEnum());
            }
        });
    }

    protected Object handle(){
        String methodName = BEFORE_MAP.get(getSignature());
        if(Objects.nonNull(methodName) && !methodName.isEmpty()){
            handle(methodName);
        }else{
            System.out.println("method not found in before map = "+method.getName());
        }
        return proceed();
    }

    protected ProceedingJoinPoint(Object target, List<Signature> list,boolean jdkProxy){
        this.target = target;
        this.jdkProxy = jdkProxy;
        setMethod(list);
    }

    protected Object preHandle(){
        String proxyMethod = BEFORE_MAP.get(getSignature());
        if(Objects.nonNull(proxyMethod) && !proxyMethod.isEmpty()){
            Class<?>[] argTypes = new Class[]{this.getClass().getSuperclass().getSuperclass()};
            Object[] args = new Object[]{this};
            return handle(proxyMethod, argTypes, args);
        }else{
            return proceed();
        }
    }

    private void afterReturningHandle(Signature signature){
        String methodName = AFTER_RETURNING_MAP.get(signature);
        if(Objects.nonNull(methodName) && !methodName.isEmpty()){
            handle(methodName);
        }
    }

    private Object afterThrowingHandle(Signature signature,Exception e){
        String methodName = AFTER_THROWING_MAP.get(signature);
        if(Objects.nonNull(methodName) && !methodName.isEmpty()){
            return handle(methodName,new Class[]{Exception.class},new Object[]{e});
        }
        return null;
    }

    private void afterHandle(Signature signature){
        String methodName = AFTER_MAP.get(signature);
        if(Objects.nonNull(methodName) && !methodName.isEmpty()){
            handle(methodName);
        }
    }

    private Signature getSignature() {
        Signature info = new Signature();
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
                throw new ExceptionWrapper(e);
            }
        }
        return null;
    }
}
