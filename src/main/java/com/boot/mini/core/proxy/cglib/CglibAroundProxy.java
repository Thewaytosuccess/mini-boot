package com.boot.mini.core.proxy.cglib;

import com.boot.mini.entity.method.Signature;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author xhzy
 */
public class CglibAroundProxy extends CglibProxy {

    public CglibAroundProxy(Object target, List<Signature> info, boolean jdkProxy) {
        super(target, info, jdkProxy);
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) {
        this.method = method;
        this.args = args;
        return preHandle();
    }
}
