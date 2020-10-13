package com.mvc.util.proxy.cglib;

import com.mvc.entity.method.MethodInfo;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author xhzy
 */
public class CglibAroundProxy extends CglibProxy {

    public CglibAroundProxy(Object target, List<MethodInfo> info, String[] methods, boolean jdkProxy) {
        super(target, info, methods, jdkProxy);
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) {
        this.method = method;
        this.args = args;
        return preHandle();
    }
}
