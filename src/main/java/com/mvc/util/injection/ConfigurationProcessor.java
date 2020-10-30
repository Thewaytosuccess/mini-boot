package com.mvc.util.injection;

import com.mvc.annotation.config.ConfigurationProperties;
import com.mvc.annotation.config.Value;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.mapping.HandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.mvc.enums.constant.ConstantPool.*;

/**
 * @author xhzy
 */
public class ConfigurationProcessor {

    private static final ConfigurationProcessor PROCESSOR = new ConfigurationProcessor();

    private ConfigurationProcessor(){}

    public static ConfigurationProcessor getInstance(){
        return PROCESSOR;
    }

    private Properties properties;

    private Map<String,Object> configMap;

    /**
     * 加载properties配置文件
     * @param configLocation 配置文件位置
     * @return 配置
     */
    public Properties loadConfig(String configLocation) {
        if(Objects.isNull(configLocation) || configLocation.isEmpty()){
            configLocation = CONFIG_PATH;
        }
        try (InputStream stream = HandlerMapping.class.getClassLoader().getResourceAsStream(configLocation)){
            if(Objects.nonNull(stream)){
                if(Objects.isNull(properties)){
                    properties = new Properties();
                }
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new ExceptionWrapper(e);
        }
        return properties;
    }

    /**
     * 注入配置
     * @param instance 实例
     */
    public void inject(Object instance){
        if (instance.getClass().isAnnotationPresent(ConfigurationProperties.class)){
            injectByName(instance);
        } else {
            injectByAnnotation(instance);
        }
    }

    private void injectByName(Object instance){
        ConfigurationProperties config = instance.getClass().getAnnotation(ConfigurationProperties.class);
        String prefix = config.prefix();
        if(prefix.isEmpty()){
            throw new RuntimeException();
        }
        if(Objects.isNull(configMap)){
            configMap = new HashMap<>(16);
        }

        if(!configMap.isEmpty()){
            configMap.clear();
        }
        properties.forEach((k, v) -> {
            if(String.valueOf(k).startsWith(prefix)){
                configMap.put(String.valueOf(k),v);
            }
        });
        setConfigValue(instance,prefix);
    }

    private void injectByAnnotation(Object instance){
        if(Objects.isNull(configMap)){
            configMap = new HashMap<>(16);
        }
        if(configMap.size() != properties.size()){
            properties.forEach((k, v) -> configMap.put(String.valueOf(k),v));
        }
        setConfigValue(instance);
    }

    private void setConfigValue(Object instance,String prefix){
        Field[] declaredFields = instance.getClass().getDeclaredFields();
        StringBuilder name = new StringBuilder(prefix);
        if(!name.toString().endsWith(PATH_SEPARATOR)){
            name.append(PATH_SEPARATOR);
        }
        //中划线转化为驼峰命名法
        strikeThroughToCamelCase();

        Object value;
        for(Field f:declaredFields){
            //有注解，优先使用注解注入，否则尝试使用字段名称作为后缀注入
            if(f.isAnnotationPresent(Value.class)){
                setValueByAnnotation(f,instance);
            }else{
                try {
                    value = configMap.get(name.append(f.getName()).toString());
                    if(Objects.nonNull(value)){
                        f.setAccessible(true);
                        f.set(instance,value);
                    }
                    name.delete(0,name.length());
                    name.append(prefix);
                    if(!name.toString().endsWith(PATH_SEPARATOR)){
                        name.append(PATH_SEPARATOR);
                    }
                } catch (IllegalAccessException e) {
                    throw new ExceptionWrapper(e);
                }
            }
        }
    }

    /**
     * 将配置名称的中划线转化为驼峰命名法
     */
    private void strikeThroughToCamelCase(){
        Map<String,Object> map = new ConcurrentHashMap<>(16);
        StringBuilder builder = new StringBuilder();
        configMap.forEach((k, v) -> {
            if(k.contains(PATH_SEPARATOR)){
                if((k.length() - 1) != k.lastIndexOf(PATH_SEPARATOR)){
                    String suffix = k.substring(k.lastIndexOf(PATH_SEPARATOR) + 1);
                    if(suffix.contains(STRIKE_THROUGH)){
                        String[] split = suffix.split(STRIKE_THROUGH);
                        for(String s:split){
                            if(s.length() > 0){
                                builder.append(s.substring(0,1).toUpperCase());
                                if(s.length() > 1){
                                    builder.append(s.substring(1));
                                }
                            }
                        }
                        if(builder.length() > 0){
                            String key = builder.toString().substring(0,1).toLowerCase();
                            if(builder.length() > 1){
                                key += builder.toString().substring(1);
                                key = k.substring(0,k.lastIndexOf(PATH_SEPARATOR) + 1) + key;
                            }
                            //兼容@Value和@ConfigurationProperties
                            map.put(key,v);
                            map.put(k,v);
                            builder.delete(0,builder.length());
                        }
                    }else{
                        map.put(k,v);
                    }
                }
            }
        });
        configMap = map;
    }

    private void setConfigValue(Object instance){
        Field[] declaredFields = instance.getClass().getDeclaredFields();
        for(Field f:declaredFields) {
            if (f.isAnnotationPresent(Value.class)) {
                setValueByAnnotation(f,instance);
            }
        }
    }

    private void setValueByAnnotation(Field f,Object instance){
        Value value = f.getAnnotation(Value.class);
        String key = value.value();
        try{
            f.setAccessible(true);
            if (key.startsWith(KEY_PREFIX) && key.endsWith(KEY_SUFFIX)) {
                key = key.substring(2, key.length() - 1);
                f.set(instance, configMap.get(key));
            } else {
                f.set(instance,key);
            }
        }catch (Exception e){
            throw new ExceptionWrapper(e);
        }
    }

    @Deprecated
    public void setValue(String key,Object value,Field f,Object instance){
        //直接将注解中的默认值注入进去
        if (!key.isEmpty()) {
            String name = f.getName();
            try {
                //调用set方法将key注入进去
                String setter = "set" + name.substring(0, 1).toUpperCase();
                if(name.length() > 1){
                    setter += name.substring(1);
                }
                Method[] methods = instance.getClass().getDeclaredMethods();
                for(Method m:methods){
                    if(m.getName().equals(setter)){
                        if(m.getParameterCount() == 1 && m.getParameterTypes()[0] == f.getType()){
                            m.invoke(instance,value);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }
    }

    @Deprecated
    public void print(Object obj){
        Class<?> clazz = obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for(Method m:methods){
            try {
                if(m.getName().startsWith("get")){
                    System.out.println(m.getName()+" >>> "+m.invoke(obj));
                }
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }
    }


    public Map<String,Object> getByPrefix(String prefix) {
        if(Objects.isNull(configMap)){
            configMap = new HashMap<>(16);
        }
        properties.forEach((k, v) -> {
            if(String.valueOf(k).startsWith(prefix)){
                configMap.put(String.valueOf(k),v);
            }
        });
        return configMap;
    }
}
