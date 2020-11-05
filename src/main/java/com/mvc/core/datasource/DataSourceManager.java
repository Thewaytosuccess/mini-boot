package com.mvc.core.datasource;

import com.mvc.annotation.enable.EnableDataSourceAutoConfiguration;
import com.mvc.annotation.jpa.Column;
import com.mvc.annotation.jpa.Id;
import com.mvc.annotation.jpa.Table;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.IocContainer;
import com.mvc.core.mapping.PackageScanner;
import com.mvc.enums.ExceptionEnum;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class DataSourceManager {

    private static final DataSourceManager MANAGER = new DataSourceManager();

    private DataSourceManager(){}

    public static DataSourceManager getInstance(){ return MANAGER; }

    private Map<Class<?>,Map<String,Class<?>>> tableMap;

    public void tableScan(){
        PackageScanner packageScanner = PackageScanner.getInstance();
        AtomicBoolean global = new AtomicBoolean(false);
        Optional.ofNullable(packageScanner.getStarterClass()).ifPresent(e ->
                global.set(e.isAnnotationPresent(EnableDataSourceAutoConfiguration.class)));
        if(global.get()){
            packageScanner.getAllClasses().stream().filter(e -> e.isAnnotationPresent(Table.class))
                    .collect(Collectors.toSet()).forEach(this::generateSql);
        }
    }

    private void generateSql(Class<?> clazz) {
        Set<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(e -> e.isAnnotationPresent(Column.class))
                .collect(Collectors.toSet());
        List<Field> ids = fields.stream().filter(e -> e.isAnnotationPresent(Id.class)).collect(Collectors.toList());
        int size = ids.size();
        if(size > 1){
            throw new ExceptionWrapper(ExceptionEnum.ID_DUPLICATED);
        }else if(size == 0){
            throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
        }
        Field id = ids.get(0);
        Map<String,Class<?>> nameTypeMap = new HashMap<>(16);
        for(Field f:fields){
            nameTypeMap.put(f.getName(),f.getType());
        }
    }



}
