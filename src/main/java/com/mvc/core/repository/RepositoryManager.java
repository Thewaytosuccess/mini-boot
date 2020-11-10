package com.mvc.core.repository;

import com.mvc.core.datasource.mapper.impl.BaseMapperImpl;
import com.mvc.core.mapping.PackageScanner;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author xhzy
 */
public class RepositoryManager {

    Map<Class<?>,Class<?>> map;

    public Map<Class<?>,Class<?>> getMap(){
        if(Objects.isNull(map)){
            map = new HashMap<>();
        }
        return map;
    }

    /**
     * 建立repository和泛型的映射
     */
    public void buildMapping(){
        PackageScanner.getInstance().getRepositories().stream().filter(e -> e.getSuperclass() == BaseMapperImpl.class)
        .forEach(this::getGeneric);
    }

    private void getGeneric(Class<?> clazz){
        Type t = clazz.getGenericSuperclass();
        if(t instanceof ParameterizedType){
            ParameterizedType type = (ParameterizedType)t;
            getMap().put(clazz,(Class<?>)type.getActualTypeArguments()[0]);
        }
    }
}
