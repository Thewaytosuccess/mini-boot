package com.mvc.core.starter;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author xhzy
 */
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContextInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext) {
        List<WebApplicationInitializer> initializers = new LinkedList<>();
        if(Objects.nonNull(set)){
            for(Class<?> clazz:set){
                try {
                    if(!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())){
                        initializers.add((WebApplicationInitializer) clazz.newInstance());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{
            try {
                initializers.add(SpringBootApplicationInitializer.class.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for(WebApplicationInitializer initializer:initializers){
            initializer.onStartUp(servletContext);
        }
    }
}
