package com.mvc.core.datasource.db;

import com.mvc.annotation.enable.EnableDataSourceAutoConfiguration;
import com.mvc.annotation.jpa.Column;
import com.mvc.annotation.jpa.Id;
import com.mvc.annotation.jpa.PrimaryKey;
import com.mvc.annotation.jpa.Table;
import com.mvc.core.datasource.connection.ConnectionManager;
import com.mvc.core.datasource.db.generator.TableGenerator;
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

    public void init(){
        PackageScanner packageScanner = PackageScanner.getInstance();
        AtomicBoolean enabled = new AtomicBoolean(false);
        Optional.ofNullable(packageScanner.getStarterClass()).ifPresent(e ->
                enabled.set(e.isAnnotationPresent(EnableDataSourceAutoConfiguration.class)));
        if(enabled.get()){
            Set<Class<?>> tables = packageScanner.getAllClasses().stream().filter(e -> e.isAnnotationPresent(Table.class))
                    .collect(Collectors.toSet());
            tables.forEach(this::getColumns);
            //try connect database
            ConnectionManager.getInstance().init();
            //create table
            TableGenerator generator = TableGenerator.getInstance();
            tables.forEach(generator::createTable);
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


}
