package com.mvc.util.injection;

import com.mvc.annotation.bean.Autowired;
import com.mvc.annotation.bean.Bean;
import com.mvc.annotation.bean.Qualifier;
import com.mvc.annotation.bean.Resource;
import com.mvc.annotation.type.Component;
import com.mvc.annotation.type.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xhzy
 */
public class DependencyInjectProcessor {

    /**
     * IOC容器
     */
    private static final Map<Class<?>,Object> IOC_CONTAINER = new ConcurrentHashMap<>();

    /**
     * 接口和对应的子类映射
     */
    private static final Map<Class<?>, List<Class<?>>> INTERFACE_INSTANCE_MAP = new ConcurrentHashMap<>();

    /**
     * 类名和类的映射
     */
    private static final Map<String,Class<?>> NAME_CLASS_MAP = new ConcurrentHashMap<>();

    public static void inject(Class<?> clazz) throws Exception{
        Object instance = clazz.newInstance();
        //1.先注入配置
        ConfigInjectProcessor.configInject(instance);
        //2.再注入bean
        beanInject(instance);
        //3.最后注入依赖的成员对象
        fieldsInject(instance,Arrays.asList(instance.getClass().getDeclaredFields()),true);
    }

    public static void reInject(Class<?> clazz,Set<Class<?>> set){
        Field[] declaredFields = clazz.getDeclaredFields();
        List<Field> fields = new ArrayList<>();
        for(Field f:declaredFields){
            if(set.contains(f.getType())){
                fields.add(f);
            }
        }
        if(!fields.isEmpty()){
            fieldsInject(getInstance(clazz),fields,false);
        }
    }

    public static Object getInstance(Class<?> clazz){
        return IOC_CONTAINER.get(clazz);
    }

    public static void replace(Class<?> clazz, Object proxy) {
        IOC_CONTAINER.put(clazz,proxy);
    }

    private static void beanInject(Object instance) throws Exception {
        Method[] declaredMethods = instance.getClass().getDeclaredMethods();
        for(Method m:declaredMethods){
            if(m.isAnnotationPresent(Bean.class)){
                Class<?>[] parameterTypes = m.getParameterTypes();
                int parameterCount = m.getParameterCount();
                if(parameterCount > 0){
                    Object[] parameters = new Object[parameterCount];
                    int i=0;
                    for(Class<?> c:parameterTypes){
                        parameters[i++] = IOC_CONTAINER.get(c);
                    }
                    IOC_CONTAINER.put(m.getReturnType(),m.invoke(instance,parameters));
                }else{
                    IOC_CONTAINER.put(m.getReturnType(),m.invoke(instance));
                }
            }
        }
    }

    /**
     * 将当前类依赖的实例注入到当前类中，setter注入
     * 同时将当前类注入到ioc容器中
     * @param instance 当前类的实例
     */
    private static void fieldsInject(Object instance,List<Field> fields,boolean injectSelf){
        //先将依赖的对象注入进来
        for(Field f:fields){
            if(f.isAnnotationPresent(Autowired.class) || f.isAnnotationPresent(Resource.class)){
                try {
                    //修改private/final修饰的字段时，一定要加上setAccessible(true)
                    f.setAccessible(true);
                    if(Objects.nonNull(IOC_CONTAINER.get(f.getType()))){
                        //反射不会自动装拆箱,尽量统一用set
                        f.set(instance,IOC_CONTAINER.get(f.getType()));
                    }else{
                        //尝试通过接口查询对应的子类
                        List<Class<?>> children = INTERFACE_INSTANCE_MAP.get(f.getType());
                        if(children.size() == 1){
                            //接口仅有一个实现类
                            f.set(instance, IOC_CONTAINER.get(children.get(0)));
                        }else{
                            //接口有多个实现类,通过名称匹配
                            f.set(instance,IOC_CONTAINER.get(getClassByName(f)));
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

    private static void injectSelf(Object instance){
        Class<?> clazz = instance.getClass();
        //将类和对应的实例注入到ioc容器
        IOC_CONTAINER.put(clazz, instance);
        Class<?>[] interfaces = clazz.getInterfaces();
        for(Class<?> interfaceClass:interfaces){
            List<Class<?>> children = INTERFACE_INSTANCE_MAP.get(interfaceClass);
            if(Objects.isNull(children)){
                children = new ArrayList<>();
            }
            children.add(clazz);
            //建立接口和子类的映射关系，以便通过接口来查询到对应的子类
            INTERFACE_INSTANCE_MAP.put(interfaceClass,children);
        }
        String className = getClassName(clazz);
        if(!className.isEmpty()){
            //建立类名和类的映射关系，当接口有多个实现类时，可以通过指定的类名来决定需要注入哪个子类
            NAME_CLASS_MAP.put(className,clazz);
        }
    }

    /**
     * 当接口有多个实现类时
     * 通过指定的类名来决定需要实例化哪个子类
     * @param f 注解的字段
     * @return 需要实例化的子类
     */
    private static Class<?> getClassByName(Field f){
        String name;
        if(f.isAnnotationPresent(Resource.class)){
            name = f.getAnnotation(Resource.class).name();
        }else{
            if(f.isAnnotationPresent(Qualifier.class)){
                name = f.getAnnotation(Qualifier.class).name();
            }else{
                throw new RuntimeException();
            }
        }
        if(name.isEmpty()){
            throw new RuntimeException();
        }
        Class<?> clazz = NAME_CLASS_MAP.get(name);
        if(Objects.isNull(clazz)){
            throw new RuntimeException();
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
    private static String getClassName(Class<?> clazz) {
        if(clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Component.class)){
            Service service = clazz.getAnnotation(Service.class);
            String name;
            if(Objects.nonNull(service)){
                name = service.name();
            }else{
                name = clazz.getAnnotation(Component.class).name();
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
