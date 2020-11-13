package com.mvc.core.repository;

import com.mvc.annotation.jpa.Id;
import com.mvc.annotation.jpa.PrimaryKey;
import com.mvc.core.datasource.db.DataSourceManager;
import com.mvc.core.datasource.mapper.impl.BaseMapperImpl;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.DependencyInjectProcessor;
import com.mvc.core.injection.IocContainer;
import com.mvc.core.mapping.PackageScanner;
import com.mvc.core.proxy.mapper.MapperProxy;
import com.mvc.enums.ExceptionEnum;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class RepositoryManager {

    private static final RepositoryManager MANAGER = new RepositoryManager();

    private RepositoryManager(){}

    public static RepositoryManager getInstance(){ return MANAGER; }

    private Map<Class<?>,Class<?>> mapping;

    public Map<Class<?>,Class<?>> getMapping(){
        if(Objects.isNull(mapping)){
            mapping = new HashMap<>(16);
        }
        return mapping;
    }

    public boolean rejected(){
        return Objects.nonNull(mapping) && !mapping.isEmpty();
    }

    public Set<Class<?>> getReInjected(){
        if(rejected()){
            return mapping.keySet();
        }
        return null;
    }

    /**
     * 实体扫描
     */
    public void scanEntities(){
        List<Class<?>> entities = new ArrayList<>();
        PackageScanner.getInstance().getRepositories().stream().filter(e -> e.getSuperclass() == BaseMapperImpl.class)
        .forEach(e -> {
            //获取repository对应的泛型
            getGeneric(e,entities);
            //创建代理，并重新注入到ioc容器
            IocContainer.getInstance().addInstance(e,new MapperProxy(e).getProxy());
        });

        DependencyInjectProcessor.getInstance().reInjectRepository();
        Map<Class<?>, List<Field>> tableMap = DataSourceManager.getInstance().getTableMap();
        entities.forEach(e -> {
            List<Field> fields = tableMap.get(e);
            if(Objects.isNull(fields) || fields.isEmpty()){
                tableMap.put(e,getPrimaryKey(e));
            }
        });
    }

    private List<Field> getPrimaryKey(Class<?> clazz) {
        List<Field> ids = Arrays.stream(clazz.getDeclaredFields()).filter(e -> e.isAnnotationPresent(PrimaryKey.class) ||
                e.isAnnotationPresent(Id.class)).collect(Collectors.toList());
        if(ids.size() == 0){
            throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
        }
        if(ids.size() > 1){
            throw new ExceptionWrapper(ExceptionEnum.ID_DUPLICATED);
        }
        return ids;
    }

    private void getGeneric(Class<?> clazz,List<Class<?>> entities){
        Type t = clazz.getGenericSuperclass();
        if(t instanceof ParameterizedType){
            ParameterizedType type = (ParameterizedType)t;
            entities.add((Class<?>)type.getActualTypeArguments()[0]);
            mapping = getMapping();
            mapping.put(clazz,(Class<?>)type.getActualTypeArguments()[0]);
        }
    }
}
