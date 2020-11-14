package com.boot.mini.base;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class CarProxy implements InvocationHandler {

    private final Object target;

    public CarProxy(Object target){
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("proxy runs....");
        return method.invoke(target,args);
    }
}
