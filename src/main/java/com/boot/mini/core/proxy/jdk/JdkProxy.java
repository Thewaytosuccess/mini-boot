package com.boot.mini.core.proxy.jdk;

import com.boot.mini.core.proxy.ProceedingJoinPoint;
import com.boot.mini.entity.method.Signature;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author xhzy
 */
public class JdkProxy extends ProceedingJoinPoint implements InvocationHandler {

    public JdkProxy(Object target, List<Signature> info, boolean jdkProxy) {
        super(target, info, jdkProxy);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args){
        this.method = method;
        this.args = args;
        return handle();
    }

}
