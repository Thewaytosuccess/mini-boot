package com.mvc.core.mapping;

import com.mvc.annotation.aop.aspect.Interceptor;
import com.mvc.annotation.config.Configuration;
import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.annotation.type.component.Component;
import com.mvc.annotation.type.component.ComponentScan;
import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.annotation.type.service.Service;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.IocContainer;
import com.mvc.enums.ExceptionEnum;

import java.io.File;
import java.net.URL;
import java.util.*;

import static com.mvc.enums.constant.ConstantPool.*;

/**
 * @author xhzy
 */
public class PackageScanner {

    private static final PackageScanner SCANNER = new PackageScanner();

    private PackageScanner(){}

    public static PackageScanner getInstance(){ return SCANNER; }

    private Set<String> paths;

    private Class<?> starterClass;

    public void scan(String basePackage){
        //1.包扫描,basePackage为空时，扫描项目根路径下的所有文件
        packageScan(basePackage);
        if(basePackage.isEmpty()){
            removeDots();
            packageRescan();
        }
        //2.解析包含注解的类并按照优先级排序
        sortAndFilter();
    }

    public Class<?> getStarterClass(){
        return starterClass;
    }

    /**
     * 解析包含注解的类并按照优先级排序,同时过滤掉不含注解的类
     * 注解优先级：@Configuration > @Component > @Service > @Controller/@RestController
     */
    private void sortAndFilter(){
        List<Class<?>> configurationClasses = new ArrayList<>();
        List<Class<?>> componentClasses = new ArrayList<>();
        List<Class<?>> serviceClasses = new ArrayList<>();
        List<Class<?>> controllerClasses = new ArrayList<>();
        List<Class<?>> restControllerClasses = new ArrayList<>();
        List<Class<?>> interceptorClasses = new ArrayList<>();
        List<Class<?>> controllerAdvice = new ArrayList<>();
        List<Class<?>> application = new ArrayList<>();

        paths.forEach(e -> {
            try {
                Class<?> clazz = Class.forName(e);
                if(clazz.isAnnotationPresent(Configuration.class)){
                    configurationClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(Component.class)){
                    componentClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(Service.class)){
                    serviceClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(RestController.class)){
                    restControllerClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(Controller.class)){
                    controllerClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(ControllerAdvice.class)){
                    controllerAdvice.add(clazz);
                }else if(clazz.isAnnotationPresent(Interceptor.class)){
                    interceptorClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(SpringBootApplication.class)){
                    application.add(clazz);
                }
            } catch (ClassNotFoundException ex) {
                throw new ExceptionWrapper(ex);
            }
        });

        if(controllerAdvice.size() > 1){
            throw new ExceptionWrapper(ExceptionEnum.CONTROLLER_ADVICE_DUPLICATED);
        }

        if(application.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.STARTER_NOT_FOUND);
        }else if(application.size() > 1){
            throw new ExceptionWrapper(ExceptionEnum.STARTER_DUPLICATED);
        }
        starterClass = application.get(0);

        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        classes.addAll(configurationClasses);
        classes.addAll(componentClasses);
        classes.addAll(serviceClasses);
        classes.addAll(restControllerClasses);
        classes.addAll(controllerClasses);
        classes.addAll(interceptorClasses);
        //倒数第二个作为启动类
        classes.add(starterClass);
        //倒数第一个作为异常处理类
        classes.addAll(controllerAdvice);
    }

    /**
     * 扫描指定包下的所有类
     */
    private void packageScan(String basePackage) {
        URL resource = this.getClass().getClassLoader().getResource(URL_SEPARATOR + basePackage.
                replaceAll("\\.", URL_SEPARATOR));
        if(Objects.nonNull(resource)){
            File root = new File(resource.getFile());
            File[] files = root.listFiles();
            if(Objects.nonNull(files) && files.length > 0){
                String dirOrFile;
                for(File f:files){
                    dirOrFile = basePackage + PATH_SEPARATOR + f.getName();
                    if(f.isDirectory()){
                        packageScan(dirOrFile);
                    }else{
                        if(dirOrFile.endsWith(".class")){
                            if(Objects.isNull(paths)){
                                paths = new HashSet<>();
                            }
                            paths.add(dirOrFile.replace(".class",""));
                        }
                    }
                }
            }
        }
    }

    /**
     * 如果没有配置需要扫描的包，默认扫描项目路径下的所有文件
     * 此时扫描的文件路径以点开头，需要去掉
     */
    private void removeDots(){
        Set<String> list = new HashSet<>();
        paths.stream().filter(e -> e.startsWith(PATH_SEPARATOR)).forEach(e -> list.add(e.substring(1)));
        if(!list.isEmpty()){
            paths = list;
        }
    }

    private void packageRescan(){
        //find SpringBootApplication
        List<Class<?>> application = new ArrayList<>();
        for(String path:paths){
            try {
                starterClass = Class.forName(path);
                if(starterClass.isAnnotationPresent(SpringBootApplication.class)){
                    application.add(starterClass);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        if(application.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.STARTER_NOT_FOUND);
        }else if(application.size() > 1){
            throw new ExceptionWrapper(ExceptionEnum.STARTER_DUPLICATED);
        }

        starterClass = application.get(0);
        String basePackage;
        if(starterClass.isAnnotationPresent(ComponentScan.class)){
            basePackage = starterClass.getAnnotation(ComponentScan.class).basePackages();
        }else{
            basePackage = starterClass.getPackage().getName();
        }

        if(Objects.nonNull(paths) && !paths.isEmpty()){
            paths.clear();
        }
        packageScan(basePackage);
    }

}
