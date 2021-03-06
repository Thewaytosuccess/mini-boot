package com.boot.mini.core.mapping;

import com.boot.mini.annotation.aop.aspect.Interceptor;
import com.boot.mini.annotation.config.Configuration;
import com.boot.mini.annotation.type.SpringBootApplication;
import com.boot.mini.annotation.type.component.Component;
import com.boot.mini.annotation.type.component.ComponentScan;
import com.boot.mini.annotation.type.controller.RestController;
import com.boot.mini.annotation.type.repository.Repository;
import com.boot.mini.core.injection.IocContainer;
import com.boot.mini.annotation.exception.ControllerAdvice;
import com.boot.mini.annotation.type.controller.Controller;
import com.boot.mini.annotation.type.service.Service;
import com.boot.mini.core.exception.ExceptionWrapper;
import com.boot.mini.enums.ExceptionEnum;

import java.io.File;
import java.net.URL;
import java.util.*;

import static com.boot.mini.enums.constant.ConstantPool.*;

/**
 * @author xhzy
 */
public class PackageScanner {

    private static final PackageScanner SCANNER = new PackageScanner();

    private PackageScanner(){}

    public static PackageScanner getInstance(){ return SCANNER; }

    private Set<String> paths;

    private Set<Class<?>> allClasses;

    private Class<?> starterClass;

    private List<Class<?>> repositories;

    private List<Class<?>> controllers;

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

    public Set<Class<?>> getAllClasses(){
        if(Objects.isNull(allClasses)){
            allClasses = new HashSet<>();
        }
        return allClasses;
    }

    public List<Class<?>> getRepositories(){
        if(Objects.isNull(repositories)){
            repositories = new ArrayList<>();
        }
        return repositories;
    }

    public List<Class<?>> getControllers(){
        if(Objects.isNull(controllers)){
            controllers = new ArrayList<>();
        }
        return controllers;
    }

    /**
     * 解析包含注解的类并按照优先级排序,同时过滤掉不含注解的类
     * 注解优先级：@Configuration > @Component > @Service > @Controller/@RestController
     */
    private void sortAndFilter(){
        List<Class<?>> configurationClasses = new ArrayList<>();
        List<Class<?>> componentClasses = new ArrayList<>();
        List<Class<?>> serviceClasses = new ArrayList<>();
        List<Class<?>> interceptorClasses = new ArrayList<>();
        List<Class<?>> controllerAdvice = new ArrayList<>();
        List<Class<?>> application = new ArrayList<>();

        allClasses = getAllClasses();
        boolean empty = true;
        if(!allClasses.isEmpty()){
            empty = false;
        }
        boolean finalEmpty = empty;
        paths.forEach(e -> {
            try {
                Class<?> clazz = Class.forName(e);
                if(finalEmpty){
                    allClasses.add(clazz);
                }
                if(clazz.isAnnotationPresent(Configuration.class)){
                    configurationClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(Component.class)){
                    componentClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(Service.class)){
                    serviceClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(RestController.class) || clazz.isAnnotationPresent(Controller.class)){
                    controllers = getControllers();
                    controllers.add(clazz);
                }else if(clazz.isAnnotationPresent(ControllerAdvice.class)){
                    controllerAdvice.add(clazz);
                }else if(clazz.isAnnotationPresent(Interceptor.class)){
                    interceptorClasses.add(clazz);
                }else if(clazz.isAnnotationPresent(SpringBootApplication.class)){
                    application.add(clazz);
                }else if(clazz.isAnnotationPresent(Repository.class)){
                    repositories = getRepositories();
                    repositories.add(clazz);
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
        classes.addAll(repositories);
        classes.addAll(componentClasses);
        classes.addAll(serviceClasses);
        classes.addAll(controllers);
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
        Set<Class<?>> allClasses = getAllClasses();
        for(String path:paths){
            try {
                starterClass = Class.forName(path);
                allClasses.add(starterClass);
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
