package com.boot.mini.core.proxy.cglib;

import com.boot.mini.entity.method.Signature;
import com.boot.mini.core.proxy.ProceedingJoinPoint;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * 原理：为被代理类产生一个子类，通过子类调用父类的方法
 * @author xhzy
 */
public class CglibProxy extends ProceedingJoinPoint implements MethodInterceptor {

    private Enhancer enhancer ;

    public CglibProxy(Object target, List<Signature> info, boolean jdkProxy) {
        super(target, info, jdkProxy);
    }

    /**
     * 创建代理对象
     * @return 代理对象
     */
    public Object getProxy(){
        if(Objects.isNull(enhancer)){
            enhancer = new Enhancer();
        }
        //指定为某个类创建子类
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(this);
        return enhancer.create();
    }

    /**
     * 通过proxy.invokeSuper(obj,args)调用父类的方法，可以实现动态代理
     * 但被代理的对象是重新创建出来的，并不是target，所以target中本来已经注入的属性都为null；
     * @param obj 目标类的实例
     * @param method 方法对象
     * @param args 参数
     * @param proxy 代理对象
     * @return 执行结果
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {
        this.method = method;
        this.args = args;
        return handle();
    }
}
