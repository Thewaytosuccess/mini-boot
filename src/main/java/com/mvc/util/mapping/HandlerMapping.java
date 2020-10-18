package com.mvc.util.mapping;

import com.mvc.annotation.config.Configuration;
import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.annotation.method.*;
import com.mvc.annotation.param.PathVariable;
import com.mvc.annotation.param.RequestBody;
import com.mvc.annotation.param.RequestParam;
import com.mvc.annotation.type.component.Component;
import com.mvc.annotation.type.component.ComponentScan;
import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.annotation.type.service.Service;
import com.mvc.entity.method.MethodInfo;
import com.mvc.entity.method.Param;
import com.mvc.enums.HttpMethodEnum;
import com.mvc.util.aspect.AspectHandler;
import com.mvc.util.aspect.AspectProcessor;
import com.mvc.util.binding.DataBindingProcessor;
import com.mvc.util.injection.DependencyInjectProcessor;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static com.mvc.enums.constant.ConstantPool.*;

/**
 * @author xhzy
 */
public class HandlerMapping {

    private static final Map<String, MethodInfo> GET_MAP = new ConcurrentHashMap<>();

    private static final Map<String, MethodInfo> POST_MAP = new ConcurrentHashMap<>();

    private static final Map<String, MethodInfo> PUT_MAP = new ConcurrentHashMap<>();

    private static final Map<String, MethodInfo> DELETE_MAP = new ConcurrentHashMap<>();

    private static final Map<String, MethodInfo> REQUEST_MAP = new ConcurrentHashMap<>();

    private static String uriPrefix = "";

    private static Set<String> classes = new HashSet<>();

    private static final List<Class<?>> CLASSES = new CopyOnWriteArrayList<>();

    /**
     * 包扫描，将所有被注解的类和方法统一注册到注册中心
     * @param basePackage 基包
     */
    public static void scanAndInject(String basePackage){
        //1.包扫描,basePackage为空时，扫描项目根路径下的所有文件
        packageScan(basePackage);
        removeDots();
        if(basePackage.isEmpty()){
            rescan();
        }
        //2.解析包含注解的类并按照优先级排序
        sortAndFilter();
        //3.注册所有带注解的类到ioc容器
        CLASSES.forEach(HandlerMapping::inject);
        //4.切面扫描
        CLASSES.forEach(HandlerMapping::aspectScan);
        //5.判断是否需要创建代理
        AspectHandler.createProxy();
        //6.判断是否需要重新注入代理
        AspectHandler.reInject();
        //7.bean初始化
        CLASSES.forEach(DependencyInjectProcessor::initialize);
        print();
    }

    public static List<Class<?>> getClasses(){
        return CLASSES;
    }

    private static void aspectScan(Class<?> clazz) {
        if(clazz.isAnnotationPresent(Configuration.class)){
            //aop
            AspectProcessor.process(clazz);
        }else if(clazz.isAnnotationPresent(Component.class)){
            AspectProcessor.process(clazz);
        }
    }

    /**
     * 扫描@ComponentScan注解，根据basePackages重新扫描
     */
    private static void rescan(){
        String basePackage = null;
        Class<?> clazz;
        for(String e: classes){
            try {
                clazz = Class.forName(e);
                if(clazz.isAnnotationPresent(ComponentScan.class)){
                    basePackage = clazz.getAnnotation(ComponentScan.class).basePackages()[0];
                    break;
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        if(Objects.isNull(basePackage)){
            throw new RuntimeException();
        }
        classes.clear();
        packageScan(basePackage);
    }

    /**
     * 如果没有配置需要扫描的包，默认扫描项目路径下的所有文件
     * 此时扫描的文件路径以点开头，需要去掉
     */
    private static void removeDots(){
        Set<String> list = new HashSet<>();
        classes.forEach(e -> {
            if(e.startsWith(PATH_SEPARATOR) && !e.endsWith(PROPERTIES_FILE_SUFFIX)){
                list.add(e.substring(1));
            }
        });
        if(list.size() > 0){
            classes = list;
        }
    }

    /**
     * 解析包含注解的类并按照优先级排序,同时过滤掉不含注解的类
     * 注解优先级：@Configuration > @Component > @Service > @Controller/@RestController
     */
    private static void sortAndFilter(){
        List<Class<?>> configurationClasses = new ArrayList<>();
        List<Class<?>> componentClasses = new ArrayList<>();
        List<Class<?>> serviceClasses = new ArrayList<>();
        List<Class<?>> controllerClasses = new ArrayList<>();
        List<Class<?>> restControllerClasses = new ArrayList<>();
        AtomicReference<Class<?>> controllerAdvice = new AtomicReference<>();

        classes.forEach(e -> {
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
                    controllerAdvice.set(clazz);
                }
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        });

        CLASSES.addAll(configurationClasses);
        CLASSES.addAll(componentClasses);
        CLASSES.addAll(serviceClasses);
        CLASSES.addAll(restControllerClasses);
        CLASSES.addAll(controllerClasses);
        Class<?> advice = controllerAdvice.get();
        if(Objects.nonNull(advice)){
            CLASSES.add(advice);
        }
    }

    public static MethodInfo getMethodInfo(String uri,String method){
        MethodInfo methodInfo = null;
        //精确匹配
        if(HttpMethodEnum.GET.getMethod().equalsIgnoreCase(method)){
            methodInfo = GET_MAP.get(uri);
            //针对@PathVariable的模糊匹配
            if(Objects.isNull(methodInfo)){
                methodInfo = DataBindingProcessor.patternMatch(GET_MAP,uri);
            }
        }else if(HttpMethodEnum.POST.getMethod().equalsIgnoreCase(method)){
            methodInfo = POST_MAP.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = DataBindingProcessor.patternMatch(POST_MAP,uri);
            }
        }else if(HttpMethodEnum.DELETE.getMethod().equalsIgnoreCase(method)){
            methodInfo = DELETE_MAP.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = DataBindingProcessor.patternMatch(DELETE_MAP,uri);
            }
        }else if(HttpMethodEnum.PUT.getMethod().equalsIgnoreCase(method)){
            methodInfo = PUT_MAP.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = DataBindingProcessor.patternMatch(PUT_MAP,uri);
            }
        }
        if(Objects.isNull(methodInfo)){
            methodInfo = REQUEST_MAP.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = DataBindingProcessor.patternMatch(REQUEST_MAP,uri);
            }
        }
        return methodInfo;
    }

    /**
     * 扫描指定包下的所有类
     */
    private static void packageScan(String basePackage) {
        URL resource = HandlerMapping.class.getClassLoader().getResource(URL_SEPARATOR + basePackage.
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
                        classes.add(dirOrFile.replace(".class",""));
                    }
                }
            }
        }
    }

    /**
     * 解析@RequestMapping,@GetMapping,@PostMapping所注解的方法，拼接成url,
     * 统一注册到map<url,package.class.method>
     */
    private static void inject(Class<?> clazz){
        try {
            //待处理@Controller
            if(clazz.isAnnotationPresent(Configuration.class) || clazz.isAnnotationPresent(Component.class) ||
               clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(ControllerAdvice.class)){
                //ioc
                DependencyInjectProcessor.inject(clazz);
            }else if(clazz.isAnnotationPresent(RestController.class)){
                DependencyInjectProcessor.inject(clazz);
                //解析类上的@RequestMapping，拼装uri前缀
                getUriPrefix(clazz);
                //建立uri和对应的方法的映射
                methodMapping(clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getUriPrefix(Class<?> clazz){
        if(clazz.isAnnotationPresent(RequestMapping.class)){
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            uriPrefix = requestMapping.value();
            if(!uriPrefix.isEmpty() && uriPrefix.endsWith(URL_SEPARATOR)){
                uriPrefix = uriPrefix.substring(0,uriPrefix.length()-1);
            }
        }
    }

    private static String getUri(String uriSuffix){
        if(!uriSuffix.isEmpty() && !uriSuffix.startsWith(URL_SEPARATOR)){
            uriSuffix = URL_SEPARATOR + uriSuffix;
        }
        return uriPrefix + uriSuffix;
    }

    private static List<Param> parseParameters(Method method){
        //参数的注解
        Parameter[] parameters = method.getParameters();
        List<Param> params = new ArrayList<>();
        for(Parameter p:parameters){
            if(p.isAnnotationPresent(PathVariable.class)){
                String name = p.getAnnotation(PathVariable.class).value();
                if(!name.isEmpty()){
                    params.add(new Param(p.getType(),name));
                }else{
                    params.add(new Param(p.getType(),p.getName()));
                }
            }else if(p.isAnnotationPresent(RequestParam.class)){
                String name = p.getAnnotation(RequestParam.class).value();
                if(!name.isEmpty()){
                    params.add(new Param(p.getType(),name));
                }else{
                    params.add(new Param(p.getType(),p.getName()));
                }
            }else if(p.isAnnotationPresent(RequestBody.class)){
                params.add(new Param(p.getType(),null));
            }else{
                //没有注解的也要保存参数名称
                params.add(new Param(p.getType(),p.getName()));
            }
        }
        return params;
    }

    /**
     * 方法上和方法参数上的注解解析
     * @param clazz 类的字节码对象
     */
    private static void methodMapping(Class<?> clazz){
        //getDeclaredMethods可以获取到不包括继承的所有方法，而getMethods只能获取到公有方法
        Method[] methods = clazz.getDeclaredMethods();
        MethodInfo methodInfo;
        for(Method method:methods){
            methodInfo = new MethodInfo(clazz.getName() + PATH_SEPARATOR + method.getName(),parseParameters(method));
            if(method.isAnnotationPresent(GetMapping.class)){
                GET_MAP.put(getUri(method.getAnnotation(GetMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(PostMapping.class)){
                POST_MAP.put(getUri(method.getAnnotation(PostMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(DeleteMapping.class)){
                DELETE_MAP.put(getUri(method.getAnnotation(DeleteMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(PutMapping.class)){
                PUT_MAP.put(getUri(method.getAnnotation(PutMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(RequestMapping.class)){
                REQUEST_MAP.put(getUri(method.getAnnotation(RequestMapping.class).value()), methodInfo);
            }
        }
    }

    private static void print(){
        GET_MAP.forEach((k,v) -> System.out.println("Mapping GET [" + k + ":" + v + "]"));
        POST_MAP.forEach((k,v) -> System.out.println("Mapping POST [" + k + ":" + v + "]"));
        DELETE_MAP.forEach((k,v) -> System.out.println("Mapping DELETE [" + k + ":" + v + "]"));
        PUT_MAP.forEach((k,v) -> System.out.println("Mapping PUT ["+ k + ":" + v + "]"));
        REQUEST_MAP.forEach((k,v) -> System.out.println("Mapping REQUEST ["+ k + ":" + v + "]"));
    }

}
