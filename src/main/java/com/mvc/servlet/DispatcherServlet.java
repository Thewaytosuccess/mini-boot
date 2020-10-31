package com.mvc.servlet;

import com.mvc.core.task.async.TaskExecutor;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.ConfigurationProcessor;
import com.mvc.core.interceptor.InterceptorProcessor;
import com.mvc.core.task.life.LifeCycleManager;
import com.mvc.core.mapping.HandlerMapping;
import com.mvc.core.invocation.InvocationProcessor;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Properties;

/**
 * @author xhzy
 */
@WebServlet(urlPatterns = "/*",asyncSupported = true)
public class DispatcherServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) {
        //1.加载配置
        String configLocation = config.getInitParameter("contextConfigLocation");
        Properties properties = ConfigurationProcessor.getInstance().loadConfig(configLocation);
        String basePackage = properties.getProperty("component.scan.base.packages");

        //2.包扫描，将所有被注解的类和方法统一注册到IOC容器
        if(Objects.isNull(basePackage)){
            basePackage = "";
        }
        HandlerMapping.getInstance().scanAndInject(basePackage);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        doPost(req,resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        doPost(req,resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        boolean interceptorExisted = false;
        InterceptorProcessor interceptor = InterceptorProcessor.getInstance();
        if(interceptor.interceptorExisted()){
            interceptorExisted = true;
            if(!interceptor.preHandle(req,resp)){
                System.out.println("request is intercepted...");
                return;
            }
        }

        Object result = InvocationProcessor.getInstance().process(req);
        if(interceptorExisted){
            interceptor.postHandle(req,resp,result);
        }

        Exception ex = null;
        try(PrintWriter writer = resp.getWriter()){
            writer.write(Objects.nonNull(result) ? result.toString() : "result is null");
        } catch (IOException e) {
            ex = e;
            throw new ExceptionWrapper(e);
        } finally {
            if(interceptorExisted){
                interceptor.afterCompletion(req,resp,result,ex);
            }
        }
    }

    @Override
    public void destroy() {
        LifeCycleManager.getInstance().destroy();
        TaskExecutor.getInstance().shutdown();
        super.destroy();
    }
}
