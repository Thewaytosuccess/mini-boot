package com.mvc.util.exception;

import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.entity.method.Signature;
import com.mvc.enums.AdviceEnum;
import com.mvc.util.mapping.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class ControllerAdviceHandler {

    public static Set<Signature> handle(){
        Set<Signature> methods = new HashSet<>();
        AtomicReference<String> adviceMethod = new AtomicReference<>();
        HandlerMapping.getClasses().forEach(e -> {
            try {
                Class<?> clazz = Class.forName(e);
                if(clazz.isAnnotationPresent(ControllerAdvice.class)){
                    Class<? extends ExceptionHandler> handler = clazz.getAnnotation(ControllerAdvice.class)
                            .exceptionHandler();
                    if(handler != ExceptionHandler.class){
                        adviceMethod.set(handler.getName() + PATH_SEPARATOR + "handle");
                    }else{
                        Method[] declaredMethods = handler.getDeclaredMethods();
                        for(Method m:declaredMethods){
                           if(m.isAnnotationPresent(com.mvc.annotation.exception.ExceptionHandler.class)){
                               adviceMethod.set(clazz.getName() + PATH_SEPARATOR + m.getName());
                           }
                        }
                    }
                }else if(clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(
                        RestController.class)){
                    Method[] declaredMethods = clazz.getDeclaredMethods();
                    for(Method m:declaredMethods){
                        methods.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                                e + PATH_SEPARATOR + m.getName()));
                    }
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
        if(Objects.nonNull(adviceMethod.get()) && !adviceMethod.get().isEmpty()){
            methods.forEach(e -> {
                e.setAdviceEnum(AdviceEnum.AfterThrowing);
                e.setAdviceMethod(adviceMethod.get());
            });
            return methods;
        }else{
            return Collections.emptySet();
        }
    }
}
