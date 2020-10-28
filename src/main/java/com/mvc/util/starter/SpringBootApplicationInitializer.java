package com.mvc.util.starter;

import com.mvc.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * @author xhzy
 */
public class SpringBootApplicationInitializer implements WebApplicationInitializer{

    @Override
    public void onStartUp(ServletContext servletContext) {
        ServletRegistration.Dynamic registration = servletContext.addServlet(
                "dispatcherServlet", DispatcherServlet.class);
        registration.setLoadOnStartup(1);
        registration.setAsyncSupported(true);
        registration.addMapping("/*");
        registration.setInitParameter("contextConfigLocation", "application.properties");
        System.out.println("dispatcherServlet startup....");
    }
}
