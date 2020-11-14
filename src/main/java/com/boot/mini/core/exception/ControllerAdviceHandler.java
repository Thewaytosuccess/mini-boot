package com.boot.mini.core.exception;

import com.boot.mini.annotation.exception.ControllerAdvice;
import com.boot.mini.core.injection.IocContainer;
import com.boot.mini.core.mapping.PackageScanner;
import com.boot.mini.entity.method.Signature;
import com.boot.mini.enums.AdviceEnum;
import com.boot.mini.enums.constant.ConstantPool;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author xhzy
 */
public class ControllerAdviceHandler {

    private static final ControllerAdviceHandler HANDLER = new ControllerAdviceHandler();

    private ControllerAdviceHandler(){}

    public static ControllerAdviceHandler getInstance(){
        return HANDLER;
    }

    public Set<Signature> handle(){
        Set<Signature> methods = new HashSet<>();
        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        if(classes.isEmpty()){
            return methods;
        }

        String adviceMethod = null;
        Class<?> controllerAdvice = classes.get(classes.size() - 1);
        if(!controllerAdvice.isAnnotationPresent(ControllerAdvice.class)){
            if(Arrays.asList(controllerAdvice.getInterfaces()).contains(ExceptionHandler.class)){
                adviceMethod = controllerAdvice.getName() + ConstantPool.PATH_SEPARATOR + "handle";
            }else{
                return methods;
            }
        }else{
            Class<? extends ExceptionHandler> handler = controllerAdvice.getAnnotation(ControllerAdvice.class)
                    .exceptionHandler();
            if(handler != ExceptionHandler.class){
                adviceMethod = handler.getName() + ConstantPool.PATH_SEPARATOR + "handle";
            }else{
                Optional<Method> first = Arrays.stream(handler.getDeclaredMethods()).filter(m ->
                        m.isAnnotationPresent(com.boot.mini.annotation.exception.ExceptionHandler.class)).findFirst();
                if(first.isPresent()){
                    adviceMethod = controllerAdvice.getName() + ConstantPool.PATH_SEPARATOR + first.get().getName();
                }
            }
        }

        if(Objects.isNull(adviceMethod)){
            return methods;
        }

        String finalAdviceMethod = adviceMethod;
        PackageScanner.getInstance().getControllers().forEach(clazz -> Arrays.stream(clazz.getDeclaredMethods())
                .forEach(m -> methods.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                clazz.getName() + ConstantPool.PATH_SEPARATOR + m.getName(), AdviceEnum.AfterThrowing, finalAdviceMethod))));
        return methods;
    }
}
