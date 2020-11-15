package com.boot.mini.core.injection;

import com.boot.mini.annotation.aop.aspect.Interceptor;
import com.boot.mini.annotation.bean.ioc.Autowired;
import com.boot.mini.annotation.bean.ioc.Bean;
import com.boot.mini.annotation.bean.ioc.Qualifier;
import com.boot.mini.annotation.type.SpringBootApplication;
import com.boot.mini.annotation.type.component.Component;
import com.boot.mini.annotation.type.controller.RestController;
import com.boot.mini.core.interceptor.HandlerInterceptor;
import com.boot.mini.core.interceptor.InterceptorProcessor;
import com.boot.mini.annotation.bean.ioc.Resource;
import com.boot.mini.annotation.config.Configuration;
import com.boot.mini.annotation.exception.ControllerAdvice;
import com.boot.mini.annotation.type.controller.Controller;
import com.boot.mini.annotation.type.repository.Repository;
import com.boot.mini.annotation.type.service.Service;
import com.boot.mini.core.aspect.AspectProcessor;
import com.boot.mini.core.datasource.repository.RepositoryManager;
import com.boot.mini.enums.ExceptionEnum;
import com.boot.mini.core.exception.ExceptionHandler;
import com.boot.mini.core.exception.ExceptionWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class DependencyInjectProcessor {

    private static final DependencyInjectProcessor PROCESSOR = new DependencyInjectProcessor();

    private DependencyInjectProcessor(){}

    public static DependencyInjectProcessor getInstance(){
        return PROCESSOR;
    }

    /**
     * 接口和对应的子类映射
     */
    private Map<Class<?>, List<Class<?>>> interfaceImplMap;

    /**
     * 类名和类的映射
     */
    private Map<String,Class<?>> nameClassMap;

    public void inject(){
        IocContainer.getInstance().getClasses().forEach(clazz -> {
            try {
                //待处理@Controller
                if (clazz.isAnnotationPresent(Configuration.class) ||
                    clazz.isAnnotationPresent(Component.class) ||
                    clazz.isAnnotationPresent(Service.class) ||
                    clazz.isAnnotationPresent(Interceptor.class) ||
                    clazz.isAnnotationPresent(RestController.class) ||
                    clazz.isAnnotationPresent(Controller.class) ||
                    clazz.isAnnotationPresent(SpringBootApplication.class) ||
                    clazz.isAnnotationPresent(ControllerAdvice.class) ||
                    clazz.isAnnotationPresent(Repository.class)) {
                    //ioc
                    inject(clazz);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new ExceptionWrapper(ex);
            }
        });
    }

    private void inject(Class<?> clazz) throws Exception{
        Object instance = clazz.newInstance();
        //1.先注入配置
        ConfigurationProcessor.getInstance().inject(instance);
        //2.再注入bean
        beanInject(instance);
        //3.最后注入依赖的成员对象
        fieldsInject(instance,Arrays.asList(instance.getClass().getDeclaredFields()),true);
    }

    public void reInject(){
        if(AspectProcessor.getInstance().reInjected()){
            Set<Class<?>> set = new HashSet<>();
            Map<Class<?>, Class<?>[]> reInjected = AspectProcessor.getInstance().getReInjected();
            reInjected.forEach((k,v) -> {
                set.add(k);
                set.addAll(Arrays.asList(v));
            });
            if(!set.isEmpty()){
                //将代理对象重新注入到依赖它的类中
                IocContainer.getInstance().getClasses().forEach(e -> reInject(e,set));
            }
        }
    }

    public void reInjectRepository(){
        if(RepositoryManager.getInstance().rejected()){
            Set<Class<?>> set = RepositoryManager.getInstance().getReInjected();
            IocContainer.getInstance().getClasses().forEach(e -> reInject(e, set));
        }
    }

    private void reInject(Class<?> clazz,Set<Class<?>> set){
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(e -> set.contains(e.getType())).
                collect(Collectors.toList());
        if(!fields.isEmpty()){
            fieldsInject(IocContainer.getInstance().getClassInstance(clazz),fields,false);
        }
    }

    private void beanInject(Object instance) throws Exception {
        Method[] declaredMethods = instance.getClass().getDeclaredMethods();
        Class<?> returnType;
        Class<?>[] parameterTypes;
        int parameterCount;

        IocContainer container = IocContainer.getInstance();
        for(Method m:declaredMethods){
            if(m.isAnnotationPresent(Bean.class)){
                returnType= m.getReturnType();
                if(returnType == Void.class){
                    throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
                }

                //register interceptor
                if(Arrays.asList(returnType.getInterfaces()).contains(HandlerInterceptor.class) ||
                   returnType.getSuperclass() == HandlerInterceptor.class){
                    InterceptorProcessor.getInstance().register(returnType);
                }

                //register controllerAdvice
                if(Arrays.asList(returnType.getInterfaces()).contains(ExceptionHandler.class)){
                    IocContainer.getInstance().addClass(returnType);
                }
                parameterTypes = m.getParameterTypes();
                parameterCount = m.getParameterCount();
                if(parameterCount > 0){
                    Object[] parameters = new Object[parameterCount];
                    int i=0;
                    for(Class<?> c:parameterTypes){
                        parameters[i++] = container.getClassInstance(c);
                    }
                    container.addInstance(returnType,m.invoke(instance,parameters));
                }else{
                    container.addInstance(returnType,m.invoke(instance));
                }
            }
        }
    }

    /**
     * 将当前类依赖的实例注入到当前类中，setter注入
     * 同时将当前类注入到ioc容器中
     * @param instance 当前类的实例
     */
    private void fieldsInject(Object instance,List<Field> fields,boolean injectSelf){
        //先将依赖的对象注入进来
        IocContainer container = IocContainer.getInstance();
        fields.stream().filter(f -> f.isAnnotationPresent(Autowired.class) || f.isAnnotationPresent(
                Resource.class)).forEach(f -> {
            try {
                //修改private/final修饰的字段时，一定要加上setAccessible(true)
                f.setAccessible(true);
                if(Objects.nonNull(container.getClassInstance(f.getType()))){
                    //反射不会自动装拆箱,尽量统一用set
                    f.set(instance, container.getClassInstance(f.getType()));
                }else{
                    //尝试通过接口查询对应的子类
                    List<Class<?>> children = interfaceImplMap.get(f.getType());
                    if(children.size() == 1){
                        //接口仅有一个实现类
                        f.set(instance, container.getClassInstance(children.get(0)));
                    }else{
                        //接口有多个实现类,通过名称匹配
                        f.set(instance, container.getClassInstance(getClassByName(f)));
                    }
                }
            } catch (Exception ex) {
                throw new ExceptionWrapper(ex);
            }
        });

        if(injectSelf){
            //然后将自身也注入到ioc容器
            injectSelf(instance);
        }
    }

    private void injectSelf(Object instance){
        Class<?> clazz = instance.getClass();
        //将类和对应的实例注入到ioc容器
        IocContainer.getInstance().addInstance(clazz, instance);
        Class<?>[] interfaces = clazz.getInterfaces();
        for(Class<?> interfaceClass:interfaces){
            if(Objects.isNull(interfaceImplMap)){
                interfaceImplMap = new HashMap<>(16);
            }

            List<Class<?>> children = interfaceImplMap.get(interfaceClass);
            if(Objects.isNull(children)){
                children = new ArrayList<>();
            }
            children.add(clazz);
            //建立接口和子类的映射关系，以便通过接口来查询到对应的子类
            interfaceImplMap.put(interfaceClass,children);
        }
        String className = getClassName(clazz);
        if(!className.isEmpty()){
            if(Objects.isNull(nameClassMap)){
                nameClassMap = new HashMap<>(16);
            }
            //建立类名和类的映射关系，当接口有多个实现类时，可以通过指定的类名来决定需要注入哪个子类
            nameClassMap.put(className,clazz);
        }
    }

    /**
     * 当接口有多个实现类时
     * 通过指定的类名来决定需要实例化哪个子类
     * @param f 注解的字段
     * @return 需要实例化的子类
     */
    private Class<?> getClassByName(Field f){
        String name;
        if(f.isAnnotationPresent(Resource.class)){
            name = f.getAnnotation(Resource.class).name();
        }else{
            if(f.isAnnotationPresent(Qualifier.class)){
                name = f.getAnnotation(Qualifier.class).name();
            }else{
                throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
            }
        }
        if(name.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
        Class<?> clazz = nameClassMap.get(name);
        if(Objects.isNull(clazz)){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
        return clazz;
    }

    /**
     * 获取带有@Service注解或@Component注解的类的名称
     * 如果指定name，则使用指定的名称作为类名
     * 否则，将类的首字母小写后作为类名
     * @param clazz 类的字节码对象
     * @return 类名
     */
    private String getClassName(Class<?> clazz) {
        if(clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Component.class)){
            Service service = clazz.getAnnotation(Service.class);
            String name;
            if(Objects.nonNull(service)){
                name = service.name();
            }else{
                name = clazz.getAnnotation(Component.class).value();
            }
            if(name.isEmpty()){
                String simpleName = clazz.getSimpleName();
                name = simpleName.substring(0,1).toLowerCase();
                if(simpleName.length() > 1){
                    name += simpleName.substring(1);
                }
            }
            return name;
        }
        return "";
    }


}
