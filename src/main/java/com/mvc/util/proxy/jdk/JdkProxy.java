package com.mvc.util.proxy.jdk;

import com.mvc.entity.method.MethodInfo;
import com.mvc.util.proxy.ProceedingJoinPoint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author xhzy
 */
public class JdkProxy extends ProceedingJoinPoint implements InvocationHandler {

    public JdkProxy(Object target, List<MethodInfo> info, String[] methods, boolean jdkProxy) {
        super(target, info, methods, jdkProxy);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args){
        this.method = method;
        this.args = args;
        return handle();
    }

}
