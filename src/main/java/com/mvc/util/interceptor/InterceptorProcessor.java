package com.mvc.util.interceptor;

import com.mvc.annotation.aop.aspect.Interceptor;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.injection.IocContainer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class InterceptorProcessor implements HandlerInterceptor{

    private static final InterceptorProcessor PROCESSOR = new InterceptorProcessor();

    private InterceptorProcessor(){}

    public static InterceptorProcessor getInstance(){ return PROCESSOR; }

    private List<Class<?>> interceptors;

    private Set<String> excludes;

    private boolean sorted = false;

    public boolean interceptorExisted(){
        if(Objects.isNull(interceptors)){
            return false;
        }
        return !interceptors.isEmpty();
    }

    public void add(Class<?> clazz){
        if(Objects.isNull(interceptors)){
            interceptors = new ArrayList<>();
        }
        interceptors.add(clazz);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        if(!sorted){
            sortByOrder();
        }

        if(Objects.nonNull(excludes)){
            for(String uri: excludes){
                if(request.getRequestURI().equals(uri)){
                    return true;
                }
            }
        }

        for(Class<?> c: interceptors){
            try {
                if(!(boolean)c.getDeclaredMethod("preHandle", HttpServletRequest.class, HttpServletResponse.class)
                        .invoke(IocContainer.getInstance().getClassInstance(c), request, response)){
                    return false;
                }
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object result) {
        Class<?> clazz;
        for(int i = interceptors.size() - 1; i >= 0; --i){
            clazz = interceptors.get(i);
            try {
                clazz.getDeclaredMethod("postHandle", HttpServletRequest.class, HttpServletResponse.class,Object.class)
                        .invoke(IocContainer.getInstance().getClassInstance(clazz), request, response,result);
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object result,
                                Exception ex) {
        Class<?> clazz;
        for(int i = interceptors.size() - 1; i >= 0; --i){
            clazz = interceptors.get(i);
            try {
                clazz.getDeclaredMethod("afterCompletion", HttpServletRequest.class,
                        HttpServletResponse.class, Object.class, Exception.class)
                        .invoke(IocContainer.getInstance().getClassInstance(clazz), request, response,result,ex);
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }
    }

    private void sortByOrder(){
        List<OrderedInterceptor> orderedInterceptors = new ArrayList<>();
        List<Class<?>> unordered = new ArrayList<>();

        Interceptor interceptor;
        String[] excludes;
        for(Class<?> c: interceptors){
            interceptor = c.getAnnotation(Interceptor.class);
            if(interceptor.order() != 0){
                orderedInterceptors.add(new OrderedInterceptor(c,interceptor.order()));
            }else{
                unordered.add(c);
            }

            excludes = interceptor.excludes();
            if(excludes.length > 0){
                if(Objects.isNull(this.excludes)){
                    this.excludes = new HashSet<>();
                }
                this.excludes.addAll(Arrays.asList(excludes));
            }
        }
        Collections.sort(orderedInterceptors);
        interceptors.clear();
        interceptors.addAll(orderedInterceptors.stream().map(OrderedInterceptor::getClazz).collect(
                Collectors.toList()));
        interceptors.addAll(unordered);
        sorted = true;
    }
}
