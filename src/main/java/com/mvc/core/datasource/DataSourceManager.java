package com.mvc.core.datasource;

import com.mvc.annotation.enable.EnableDataSourceAutoConfiguration;
import com.mvc.annotation.jpa.Column;
import com.mvc.annotation.jpa.Id;
import com.mvc.annotation.jpa.PrimaryKey;
import com.mvc.annotation.jpa.Table;
import com.mvc.core.exception.ExceptionWrapper;
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

    private Map<Class<?>,List<Field>> tableMap;

    public Map<Class<?>, List<Field>> getTableMap() {
        if(Objects.isNull(tableMap)){
            tableMap = new HashMap<>(16);
        }
        return tableMap;
    }

    public void tableScan(){
        PackageScanner packageScanner = PackageScanner.getInstance();
        AtomicBoolean global = new AtomicBoolean(false);
        Optional.ofNullable(packageScanner.getStarterClass()).ifPresent(e ->
                global.set(e.isAnnotationPresent(EnableDataSourceAutoConfiguration.class)));
        if(global.get()){
            packageScanner.getAllClasses().stream().filter(e -> e.isAnnotationPresent(Table.class))
                    .collect(Collectors.toSet()).forEach(this::getColumns);
        }
    }

    private void getColumns(Class<?> clazz) {
        Set<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(e -> e.isAnnotationPresent(Column.class))
                .collect(Collectors.toSet());
        List<Field> ids = fields.stream().filter(e -> e.isAnnotationPresent(Id.class)).collect(Collectors.toList());

        int size = ids.size();
        boolean idExist = true;
        if(size > 1){
            throw new ExceptionWrapper(ExceptionEnum.ID_DUPLICATED);
        }else if(size == 0){
            idExist = false;
            ids = fields.stream().filter(e -> e.isAnnotationPresent(PrimaryKey.class)).collect(Collectors.toList());
            size = ids.size();
            if(size == 0){
                throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
            }
            if(size > 1){
                throw new ExceptionWrapper(ExceptionEnum.ID_DUPLICATED);
            }
        }

        List<Field> properties = new ArrayList<>();
        properties.add(ids.get(0));
        boolean finalIdExist = idExist;
        properties.addAll(fields.stream().filter(e -> finalIdExist ? !e.isAnnotationPresent(Id.class) :
                !e.isAnnotationPresent(PrimaryKey.class)).collect(Collectors.toList()));

        tableMap = getTableMap();
        tableMap.put(clazz,properties);
    }

    public String getTableName(Class<?> clazz) {
        String tableName = clazz.getAnnotation(Table.class).table();
        return tableName.isEmpty() ? mapCamelCaseToUnderscore(clazz.getSimpleName()) : tableName;
    }

    public String getColumnName(Field e) {
        String columnName = e.getAnnotation(Column.class).column();
        return columnName.isEmpty() ? mapCamelCaseToUnderscore(e.getName()) : columnName;
    }

    private String mapCamelCaseToUnderscore(String simpleName){
        StringBuilder builder = new StringBuilder(simpleName.substring(0,1).toLowerCase());
        char[] chars = simpleName.toCharArray();
        for(int i=1,len = simpleName.length(); i < len; ++i){
            while(i < len && (Character.isLowerCase(chars[i]))){
                builder.append(chars[i++]);
            }
            if(i < len){
                builder.append("_").append(Character.toLowerCase(chars[i]));
            }
        }
        return builder.toString();
    }

}
