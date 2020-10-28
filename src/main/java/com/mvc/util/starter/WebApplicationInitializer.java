package com.mvc.util.starter;

import javax.servlet.ServletContext;

/**
 * @author xhzy
 */
public interface WebApplicationInitializer {

    /**
     * startup
     * @param servletContext context
     */
    void onStartUp(ServletContext servletContext);
}
