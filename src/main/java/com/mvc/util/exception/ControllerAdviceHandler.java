package com.mvc.util.exception;

import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.entity.method.Signature;
import com.mvc.enums.AdviceEnum;
import com.mvc.util.injection.IocContainer;

import java.lang.reflect.Method;
import java.util.*;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

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
        if(Objects.isNull(classes) || classes.isEmpty()){
            return methods;
        }

        String adviceMethod = null;
        Class<?> controllerAdvice = classes.get(classes.size() - 1);
        if(!controllerAdvice.isAnnotationPresent(ControllerAdvice.class)){
            if(Arrays.asList(controllerAdvice.getInterfaces()).contains(ExceptionHandler.class)){
                adviceMethod = controllerAdvice.getName() + PATH_SEPARATOR + "handle";
            }else{
                return methods;
            }
        }else{
            Class<? extends ExceptionHandler> handler = controllerAdvice.getAnnotation(ControllerAdvice.class)
                    .exceptionHandler();
            if(handler != ExceptionHandler.class){
                adviceMethod = handler.getName() + PATH_SEPARATOR + "handle";
            }else{
                Method[] declaredMethods = handler.getDeclaredMethods();
                for(Method m:declaredMethods){
                    if(m.isAnnotationPresent(com.mvc.annotation.exception.ExceptionHandler.class)){
                        adviceMethod = controllerAdvice.getName() + PATH_SEPARATOR + m.getName();
                    }
                }
            }
        }

        if(Objects.isNull(adviceMethod) || adviceMethod.isEmpty()){
            return methods;
        }

        classes.forEach(clazz -> {
            if(clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(RestController.class)){
                Method[] declaredMethods = clazz.getDeclaredMethods();
                for(Method m:declaredMethods){
                    methods.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                            clazz.getName() + PATH_SEPARATOR + m.getName()));
                }
            }
        });
        String finalAdviceMethod = adviceMethod;
        methods.forEach(e -> {
            e.setAdviceEnum(AdviceEnum.AfterThrowing);
            e.setAdviceMethod(finalAdviceMethod);
        });
        return methods;
    }
}
