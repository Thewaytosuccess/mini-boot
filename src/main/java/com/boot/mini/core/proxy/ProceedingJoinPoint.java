package com.boot.mini.core.proxy;

import com.boot.mini.entity.method.Signature;
import com.boot.mini.enums.ExceptionEnum;
import com.boot.mini.enums.constant.ConstantPool;
import com.boot.mini.core.aspect.AspectProcessor;
import com.boot.mini.core.task.async.AsyncTaskManager;
import com.boot.mini.core.task.async.TaskExecutor;
import com.boot.mini.core.exception.ExceptionWrapper;
import com.boot.mini.core.injection.IocContainer;

import java.lang.reflect.Method;
import java.util.*;

import static com.boot.mini.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class ProceedingJoinPoint {

    private Map<Signature,String> beforeMap;

    private Map<Signature,String> afterMap;

    private Map<Signature,String> afterReturningMap;

    private Map<Signature,String> afterThrowingMap;

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
            Object result = AsyncTaskManager.getInstance().isAsync(signature) ?
                    TaskExecutor.getInstance().getExecutor().submit(this::invoke).get() : invoke();
            postHandle(signature);
            return result;
        } catch (Exception e){
            return afterThrowing(signature,e);
        } finally {
            afterCompletion(signature);
        }
    }

    private Object invoke() throws Exception{
        return jdkProxy ? method.invoke(target, args) : target.getClass().getDeclaredMethod(
                method.getName(), method.getParameterTypes()).invoke(target,args);
    }

    public void setMethod(List<Signature> methods){
        methods.forEach(e -> {
            switch (e.getAdviceEnum()){
                case Before:
                    beforeMap = getMap(beforeMap);
                    //remove duplicate
                    if(Objects.isNull(beforeMap.get(e))){
                        beforeMap.put(e,e.getAdviceMethod());
                    }
                    break;
                case After:
                    afterMap = getMap(afterMap);
                    afterMap.put(e,e.getAdviceMethod());
                    break;
                case AfterReturning:
                    afterReturningMap = getMap(afterReturningMap);
                    afterReturningMap.put(e,e.getAdviceMethod());
                    break;
                case AfterThrowing:
                    afterThrowingMap = getMap(afterThrowingMap);
                    afterThrowingMap.put(e,e.getAdviceMethod());
                    break;
                case Around:
                    beforeMap = getMap(beforeMap);
                    beforeMap.put(e,e.getAdviceMethod());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + e.getAdviceEnum());
            }
        });
    }

    private Map<Signature,String> getMap(Map<Signature,String> map){
        return Objects.isNull(map) ? new HashMap<>(16) : map;
    }

    protected Object handle(){
        if(Objects.nonNull(beforeMap) && !beforeMap.isEmpty()){
            String methodName = beforeMap.get(getSignature());
            if(Objects.nonNull(methodName) && !methodName.isEmpty()){
                handle(methodName);
            }else{
                System.out.println("method not found in before map = "+method.getName());
            }
        }
        return proceed();
    }

    protected ProceedingJoinPoint(Object target, List<Signature> list, boolean jdkProxy){
        this.target = target;
        this.jdkProxy = jdkProxy;
        setMethod(list);
    }

    protected Object preHandle(){
        if(Objects.nonNull(beforeMap) && !beforeMap.isEmpty()){
            String proxyMethod = beforeMap.get(getSignature());
            if(Objects.nonNull(proxyMethod) && !proxyMethod.isEmpty()){
                Class<?>[] argTypes = new Class[]{this.getClass().getSuperclass().getSuperclass()};
                Object[] args = new Object[]{this};
                return handle(proxyMethod, argTypes, args);
            }else{
                return proceed();
            }
        }
        return null;
    }

    private void postHandle(Signature signature){
        if(Objects.nonNull(afterMap) && !afterMap.isEmpty()){
            String methodName = afterMap.get(signature);
            if(Objects.nonNull(methodName) && !methodName.isEmpty()){
                handle(methodName);
            }
        }
    }

    private void afterCompletion(Signature signature){
        if(Objects.nonNull(afterReturningMap) && !afterReturningMap.isEmpty()){
            String methodName = afterReturningMap.get(signature);
            if(Objects.nonNull(methodName) && !methodName.isEmpty()){
                handle(methodName);
            }
        }
    }

    private Object afterThrowing(Signature signature,Exception e){
        if(Objects.nonNull(afterThrowingMap) && !afterThrowingMap.isEmpty()){
            String methodName = afterThrowingMap.get(signature);
            if(Objects.nonNull(methodName) && !methodName.isEmpty()){
                return handle(methodName,new Class[]{Exception.class},new Object[]{e});
            }
        }
        return null;
    }

    private Signature getSignature() {
        Signature info = new Signature();
        String classImpl = AspectProcessor.getInstance().getClassImpl(method.getDeclaringClass());
        if(Objects.isNull(classImpl) || classImpl.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.CLASS_IMPL_NOT_FOUND);
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
                            .invoke(IocContainer.getInstance().getClassInstance(clazz), args);
                }else{
                    return clazz.getDeclaredMethod(method.substring(index + 1)).invoke(
                            IocContainer.getInstance().getClassInstance(clazz));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExceptionWrapper(e);
            }
        }
        return null;
    }
}
