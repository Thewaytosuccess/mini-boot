package com.mvc.core.exception;

import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.core.mapping.PackageScanner;
import com.mvc.entity.method.Signature;
import com.mvc.enums.AdviceEnum;
import com.mvc.core.injection.IocContainer;

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
        if(classes.isEmpty()){
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
                Optional<Method> first = Arrays.stream(handler.getDeclaredMethods()).filter(m ->
                        m.isAnnotationPresent(com.mvc.annotation.exception.ExceptionHandler.class)).findFirst();
                if(first.isPresent()){
                    adviceMethod = controllerAdvice.getName() + PATH_SEPARATOR + first.get().getName();
                }
            }
        }

        if(Objects.isNull(adviceMethod)){
            return methods;
        }

        String finalAdviceMethod = adviceMethod;
        PackageScanner.getInstance().getControllers().forEach(clazz -> Arrays.stream(clazz.getDeclaredMethods())
                .forEach(m -> methods.add(new Signature(m.getParameterCount(),m.getParameterTypes(),
                clazz.getName() + PATH_SEPARATOR + m.getName(),AdviceEnum.AfterThrowing, finalAdviceMethod))));
        return methods;
    }
}
