package com.mvc.util.proxy.jdk;

import com.mvc.entity.method.MethodInfo;

import java.lang.reflect.Method;

/**
 * @author xhzy
 */
public class JdkAroundProxy extends JdkProxy {

    public JdkAroundProxy(Object target, MethodInfo info, String[] methods, boolean jdkProxy) {
        super(target, info, methods, jdkProxy);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)  {
       this.method = method;
       this.args = args;
       return preHandle();
    }
}
