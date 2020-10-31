package com.mvc.core.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author xhzy
 */
public interface HandlerInterceptor {

    /**
     * before invocation
     * @param request request
     * @param response response
     * @return whether the request should be intercepted
     */
    boolean preHandle(HttpServletRequest request, HttpServletResponse response);

    /**
     * after invocation
     * @param request request
     * @param response response
     * @param handler result
     */
    void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler);

    /**
     * before returning
     * @param request request
     * @param response response
     * @param handler result
     * @param e exception
     */
    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                         Exception e);
}
