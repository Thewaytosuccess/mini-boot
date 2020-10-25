package com.mvc.util.injection;

import com.mvc.annotation.bean.ioc.Autowired;
import com.mvc.annotation.bean.ioc.Bean;
import com.mvc.annotation.bean.ioc.Qualifier;
import com.mvc.annotation.bean.ioc.Resource;
import com.mvc.annotation.type.component.Component;
import com.mvc.annotation.type.service.Service;
import com.mvc.enums.ExceptionEnum;
import com.mvc.util.exception.ExceptionHandler;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.interceptor.HandlerInterceptor;
import com.mvc.util.interceptor.InterceptorProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

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
    private Map<Class<?>, List<Class<?>>> interfaceInstanceMap;

    /**
     * 类名和类的映射
     */
    private Map<String,Class<?>> nameClassMap;

    public void inject(Class<?> clazz) throws Exception{
        Object instance = clazz.newInstance();
        //1.先注入配置
        ConfigurationProcessor.getInstance().inject(instance);
        //2.再注入bean
        beanInject(instance);
        //3.最后注入依赖的成员对象
        fieldsInject(instance,Arrays.asList(instance.getClass().getDeclaredFields()),true);
    }

    public void reInject(Class<?> clazz,Set<Class<?>> set){
        Field[] declaredFields = clazz.getDeclaredFields();
        List<Field> fields = new ArrayList<>();
        for(Field f:declaredFields){
            if(set.contains(f.getType())){
                fields.add(f);
            }
        }
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
                    InterceptorProcessor.getInstance().add(returnType);
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
        for(Field f:fields){
            if(f.isAnnotationPresent(Autowired.class) || f.isAnnotationPresent(Resource.class)){
                try {
                    //修改private/final修饰的字段时，一定要加上setAccessible(true)
                    f.setAccessible(true);
                    if(Objects.nonNull(container.getClassInstance(f.getType()))){
                        //反射不会自动装拆箱,尽量统一用set
                        f.set(instance, container.getClassInstance(f.getType()));
                    }else{
                        //尝试通过接口查询对应的子类
                        List<Class<?>> children = interfaceInstanceMap.get(f.getType());
                        if(children.size() == 1){
                            //接口仅有一个实现类
                            f.set(instance, container.getClassInstance(children.get(0)));
                        }else{
                            //接口有多个实现类,通过名称匹配
                            f.set(instance, container.getClassInstance(getClassByName(f)));
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
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
            if(Objects.isNull(interfaceInstanceMap)){
                interfaceInstanceMap = new HashMap<>(16);
            }

            List<Class<?>> children = interfaceInstanceMap.get(interfaceClass);
            if(Objects.isNull(children)){
                children = new ArrayList<>();
            }
            children.add(clazz);
            //建立接口和子类的映射关系，以便通过接口来查询到对应的子类
            interfaceInstanceMap.put(interfaceClass,children);
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
