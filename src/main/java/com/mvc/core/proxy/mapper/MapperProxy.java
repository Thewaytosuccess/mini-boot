package com.mvc.core.proxy.mapper;

import com.mvc.core.repository.RepositoryManager;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author xhzy
 */
public class MapperProxy implements MethodInterceptor {

    private final Enhancer enhancer = new Enhancer();

    private final Class<?> targetClass;

    public MapperProxy(Class<?> targetClass){
        this.targetClass = targetClass;
    }

    /**
     * 创建代理对象
     * @return 代理对象
     */
    public Object getProxy(){
        //指定为某个类创建子类
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(this);
        return enhancer.create();
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        List<String> targetMethods= Collections.unmodifiableList(Arrays.asList("selectByPrimaryKey",
                "deleteByPrimaryKey"));
        if(targetMethods.contains(method.getName())){
            Object[] params = new Object[args.length + 1];
            params[0] = args[0];
            params[1] = new Class[]{RepositoryManager.getInstance().getMapping().get(targetClass)};
            return proxy.invokeSuper(o,params);
        }
        return proxy.invokeSuper(o, args);
    }
}
