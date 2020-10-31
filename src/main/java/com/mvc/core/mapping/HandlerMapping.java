package com.mvc.core.mapping;

import com.mvc.annotation.aop.aspect.Interceptor;
import com.mvc.annotation.config.Configuration;
import com.mvc.annotation.exception.ControllerAdvice;
import com.mvc.annotation.method.http.*;
import com.mvc.annotation.param.PathVariable;
import com.mvc.annotation.param.RequestBody;
import com.mvc.annotation.param.RequestParam;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.annotation.type.component.Component;
import com.mvc.annotation.type.component.ComponentScan;
import com.mvc.annotation.type.controller.Controller;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.annotation.type.service.Service;
import com.mvc.core.injection.DependencyInjectProcessor;
import com.mvc.entity.method.MethodInfo;
import com.mvc.entity.method.Param;
import com.mvc.enums.ExceptionEnum;
import com.mvc.enums.HttpMethodEnum;
import com.mvc.core.aspect.AspectHandler;
import com.mvc.core.binding.DataBindingProcessor;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.IocContainer;
import com.mvc.core.task.init.BeanInitializer;
import com.mvc.core.task.schedule.job.ScheduledJobManager;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.mvc.enums.constant.ConstantPool.*;

/**
 * @author xhzy
 */
public class HandlerMapping {

    private static final HandlerMapping HANDLER_MAPPING = new HandlerMapping();

    private HandlerMapping(){}

    public static HandlerMapping getInstance(){
        return HANDLER_MAPPING;
    }

    private Map<String, MethodInfo> getMap;

    private Map<String, MethodInfo> postMap;

    private Map<String, MethodInfo> putMap;

    private Map<String, MethodInfo> deleteMap;

    private Map<String, MethodInfo> requestMap;

    private String uriPrefix = "";

    private Set<String> paths;

    /**
     * 包扫描，将所有被注解的类和方法统一注册到注册中心
     * @param basePackage 基包
     */
    public void scanAndInject(String basePackage){
        //1.包扫描,basePackage为空时，扫描项目根路径下的所有文件
        packageScan(basePackage);
        removeDots();
        if(basePackage.isEmpty()){
            rescan();
        }
        //2.解析包含注解的类并按照优先级排序
        sortAndFilter();

        //3.注册所有带注解的类到ioc容器
        DependencyInjectProcessor injectProcessor = DependencyInjectProcessor.getInstance();
        injectProcessor.inject();
        //4.mvc建立url和方法的映射
        buildMapping(IocContainer.getInstance().getControllers());

        //5.切面扫描
        AspectHandler aspectHandler = AspectHandler.getInstance();
        aspectHandler.aspectScan();
        //6.为切面指向的类创建代理
        aspectHandler.createProxy();
        //7.将代理重新注入ioc容器
        injectProcessor.reInject();

        //8.bean初始化
        BeanInitializer.getInstance().init();
        //9.开启定时任务
        ScheduledJobManager.getInstance().init();
        print();
    }

    /**
     * 扫描@ComponentScan注解，根据basePackages重新扫描
     */
    private void rescan(){
        String basePackage = null;
        Class<?> clazz;
        for(String e: paths){
            try {
                clazz = Class.forName(e);
                if(clazz.isAnnotationPresent(SpringBootApplication.class) && clazz.isAnnotationPresent(ComponentScan.class)){
                    //简化处理
                    basePackage = clazz.getAnnotation(ComponentScan.class).basePackages()[0];
                    break;
                }
            } catch (ClassNotFoundException ex) {
                throw new ExceptionWrapper(ex);
            }
        }
        if(Objects.isNull(basePackage)){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
        paths.clear();
        packageScan(basePackage);
    }

    /**
     * 如果没有配置需要扫描的包，默认扫描项目路径下的所有文件
     * 此时扫描的文件路径以点开头，需要去掉
     */
    private void removeDots(){
        Set<String> list = new HashSet<>();
        paths.stream().filter(e -> e.startsWith(PATH_SEPARATOR) && !e.endsWith(PROPERTIES_FILE_SUFFIX))
                .forEach(e -> list.add(e.substring(1)));
        if(list.size() > 0){
            paths = list;
        }
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
        List<Class<?>> application = new ArrayList<>(1);

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
        }else if(application.size() != 1){
            throw new ExceptionWrapper(ExceptionEnum.STARTER_NOT_FOUND);
        }

        List<Class<?>> classes = IocContainer.getInstance().getClasses();
        classes.addAll(configurationClasses);
        classes.addAll(componentClasses);
        classes.addAll(serviceClasses);
        classes.addAll(restControllerClasses);
        classes.addAll(controllerClasses);
        classes.addAll(interceptorClasses);
        //倒数第二个作为启动类
        classes.addAll(application);
        //倒数第一个作为异常处理类
        classes.addAll(controllerAdvice);
    }

    public MethodInfo getMethodInfo(String uri,String method){
        MethodInfo methodInfo = null;
        //精确匹配
        DataBindingProcessor processor = DataBindingProcessor.getInstance();
        if(HttpMethodEnum.GET.getMethod().equalsIgnoreCase(method)){
            methodInfo = getMap.get(uri);
            //针对@PathVariable的模糊匹配
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(getMap,uri);
            }
        }else if(HttpMethodEnum.POST.getMethod().equalsIgnoreCase(method)){
            methodInfo = postMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(postMap,uri);
            }
        }else if(HttpMethodEnum.DELETE.getMethod().equalsIgnoreCase(method)){
            methodInfo = deleteMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(deleteMap,uri);
            }
        }else if(HttpMethodEnum.PUT.getMethod().equalsIgnoreCase(method)){
            methodInfo = putMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(putMap,uri);
            }
        }
        if(Objects.isNull(methodInfo)){
            methodInfo = requestMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(requestMap,uri);
            }
        }
        return methodInfo;
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
                if(Objects.isNull(paths)){
                    paths = new HashSet<>();
                }
                String dirOrFile;
                for(File f:files){
                    dirOrFile = basePackage + PATH_SEPARATOR + f.getName();
                    if(f.isDirectory()){
                        packageScan(dirOrFile);
                    }else{
                        paths.add(dirOrFile.replace(".class",""));
                    }
                }
            }
        }
    }

    /**
     * 解析@RequestMapping,@GetMapping,@PostMapping所注解的方法，拼接成url,
     * 统一注册到map<url,package.class.method>
     */
    private void buildMapping(List<Class<?>> classes){
        //待处理@Controller
        classes.forEach(e -> {
            //解析类上的@RequestMapping，拼装uri前缀
            getUriPrefix(e);
            //建立uri和对应的方法的映射
            methodMapping(e);
        });
    }

    private void getUriPrefix(Class<?> clazz){
        if(clazz.isAnnotationPresent(RequestMapping.class)){
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            uriPrefix = requestMapping.value();
            if(!uriPrefix.isEmpty() && uriPrefix.endsWith(URL_SEPARATOR)){
                uriPrefix = uriPrefix.substring(0,uriPrefix.length()-1);
            }
        }
    }

    private String getUri(String uriSuffix){
        if(!uriSuffix.isEmpty() && !uriSuffix.startsWith(URL_SEPARATOR)){
            uriSuffix = URL_SEPARATOR + uriSuffix;
        }
        return uriPrefix + uriSuffix;
    }

    private List<Param> parseParameters(Method method){
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
    private void methodMapping(Class<?> clazz){
        //getDeclaredMethods可以获取到不包括继承的所有方法，而getMethods只能获取到公有方法
        Method[] methods = clazz.getDeclaredMethods();
        MethodInfo methodInfo;
        for(Method method:methods){
            methodInfo = new MethodInfo(clazz.getName() + PATH_SEPARATOR + method.getName(),
                    parseParameters(method));
            if(method.isAnnotationPresent(GetMapping.class)){
                getMap = initMap(getMap);
                getMap.put(getUri(method.getAnnotation(GetMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(PostMapping.class)){
                postMap = initMap(postMap);
                postMap.put(getUri(method.getAnnotation(PostMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(DeleteMapping.class)){
                deleteMap = initMap(deleteMap);
                deleteMap.put(getUri(method.getAnnotation(DeleteMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(PutMapping.class)){
                putMap = initMap(putMap);
                putMap.put(getUri(method.getAnnotation(PutMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(RequestMapping.class)){
                requestMap = initMap(requestMap);
                requestMap.put(getUri(method.getAnnotation(RequestMapping.class).value()), methodInfo);
            }
        }
    }

    private Map<String, MethodInfo> initMap(Map<String, MethodInfo> map){
        return Objects.isNull(map) ? new HashMap<>(16) : map;
    }

    private void print(){
        if(Objects.nonNull(getMap)){
            getMap.forEach((k, v) -> System.out.println("Mapping GET [" + k + ":" + v + "]"));
        }

        if(Objects.nonNull(postMap)){
            postMap.forEach((k, v) -> System.out.println("Mapping POST [" + k + ":" + v + "]"));
        }

        if(Objects.nonNull(deleteMap)){
            deleteMap.forEach((k, v) -> System.out.println("Mapping DELETE [" + k + ":" + v + "]"));
        }

        if(Objects.nonNull(putMap)){
            putMap.forEach((k, v) -> System.out.println("Mapping PUT ["+ k + ":" + v + "]"));
        }

        if(Objects.nonNull(requestMap)){
            requestMap.forEach((k, v) -> System.out.println("Mapping REQUEST ["+ k + ":" + v + "]"));
        }
    }

}
